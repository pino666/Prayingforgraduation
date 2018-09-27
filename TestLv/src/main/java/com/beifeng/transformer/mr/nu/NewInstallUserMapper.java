package com.beifeng.transformer.mr.nu;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.log4j.Logger;

import com.beifeng.common.DateEnum;
import com.beifeng.common.EventLogConstants;
import com.beifeng.common.KpiType;
import com.beifeng.transformer.model.dim.StatsCommonDimension;
import com.beifeng.transformer.model.dim.StatsUserDimension;
import com.beifeng.transformer.model.dim.base.BrowserDimension;
import com.beifeng.transformer.model.dim.base.DateDimension;
import com.beifeng.transformer.model.dim.base.KpiDimension;
import com.beifeng.transformer.model.dim.base.PlatformDimension;
import com.beifeng.transformer.model.value.map.TimeOutputValue;

/**
 * 自定义的计算新用户的mapper类
 * 
 * @author gerry
 *
 */
public class NewInstallUserMapper extends TableMapper<StatsUserDimension, TimeOutputValue> {
    private static final Logger logger = Logger.getLogger(NewInstallUserMapper.class);

    //基本套路：先定义一个总的组合维度，比如这里的用户分析组合维度(包括新注册用户分析和总用户分析)，
    // 这个组合维度是为多个表进行结果输出的，比如这里的(stat_user和stat_device_browser)表，（ 用户基本分析和浏览器分析 ）
    // 所以他会包含多个维度组合，比如(通用维度(date,plarfotm,kpi)和browser维度),
    private StatsUserDimension statsUserDimension = new StatsUserDimension();
    private TimeOutputValue timeOutputValue = new TimeOutputValue();

    //列族
    private byte[] family = Bytes.toBytes(EventLogConstants.EVENT_LOGS_FAMILY_NAME);

    //kpi 指标   用户基本信息分析 和 浏览器信息分析 中都包含   新注册用户分析
    private KpiDimension newInstallUserKpi = new KpiDimension(KpiType.NEW_INSTALL_USER.name);
    private KpiDimension newInstallUserOfBrowserKpi = new KpiDimension(KpiType.BROWSER_NEW_INSTALL_USER.name);


    /**
     * 因为是继承的TableMapper，就是从hbase中读取数据，所以ImmutableBytesWritable和 Result 都是hase下的
     */
    @Override
    protected void map(ImmutableBytesWritable key, Result value, Context context) throws IOException, InterruptedException {

        //从hbase的中读取uuid，
        String uuid = Bytes.toString(value.getValue(family, Bytes.toBytes(EventLogConstants.LOG_COLUMN_NAME_UUID)));
        String serverTime = Bytes.toString(value.getValue(family, Bytes.toBytes(EventLogConstants.LOG_COLUMN_NAME_SERVER_TIME)));
        String platform = Bytes.toString(value.getValue(family, Bytes.toBytes(EventLogConstants.LOG_COLUMN_NAME_PLATFORM)));
        if (StringUtils.isBlank(uuid) || StringUtils.isBlank(serverTime) || StringUtils.isBlank(platform)) {
            logger.warn("uuid&servertime&platform不能为空");
            return;
        }
        long longOfTime = Long.valueOf(serverTime.trim());   //时间戳   服务器时间

        //timeOutputValue 就是写出时的 value
        timeOutputValue.setId(uuid); // 设置id为uuid
        timeOutputValue.setTime(longOfTime); // 设置时间为服务器时间

        DateDimension dateDimension = DateDimension.buildDate(longOfTime, DateEnum.DAY);
        List<PlatformDimension> platformDimensions = PlatformDimension.buildList(platform);

        /**
         * 获取 statsUserDimension 的通用维度，然后为通用维度（date，platform，kpi）赋值，这也就解释了为什么下面要for循环
         * platformDimensions ，因为有多个platform啊（一个java/js）还有一个all维度，
         *
         */
        StatsCommonDimension statsCommonDimension = this.statsUserDimension.getStatsCommon();
        statsCommonDimension.setDate(dateDimension);

        // 写browser相关的数据
        String browserName = Bytes.toString(value.getValue(family, Bytes.toBytes(EventLogConstants.LOG_COLUMN_NAME_BROWSER_NAME)));
        String browserVersion = Bytes.toString(value.getValue(family, Bytes.toBytes(EventLogConstants.LOG_COLUMN_NAME_BROWSER_VERSION)));
        List<BrowserDimension> browserDimensions = BrowserDimension.buildList(browserName, browserVersion);
        BrowserDimension defaultBrowser = new BrowserDimension("", "");


        for (PlatformDimension pf : platformDimensions) {     //遍历 platformDimensions

            /**
             * 这里为什么要设置browser为空呢？
             * 因为第一层for循环要写出的是stat_user表的新注册用户的数据，它不需要浏览器相关信息
             * 它的kpi设置的也是 new_install_user 维度信息
             */
            statsUserDimension.setBrowser(defaultBrowser);
            // 2. 解决有空的browser输出的bug
            // statsUserDimension.getBrowser().clean();
            //为剩余的两个通用维度赋值，
            statsCommonDimension.setKpi(newInstallUserKpi);
            statsCommonDimension.setPlatform(pf);

            /**
             * 结果 statsUserDimension 中只有common dimension(date,platdorm,kpi)
             * timeOutputValue id为uuid，time为server time
             * 这个写出是为用户基本信息分析中的新注册用户分析使用的，
             */
            context.write(statsUserDimension, timeOutputValue);


            /**
             * 然后再遍历browserDimensions，
             * 在第二层循环，说明一条数据进来，会发送出四条数据，
             * js chrome 1
             * js chrome all
             * all chrome 1
             * all chrome all
             * 原因是平台维度不同，一个是消息中的维度（java或js），另外一个是all维度，
             */
            for (BrowserDimension br : browserDimensions) {

                //只设置kpi名字就可以，date和platforn在第一层循环中已经设置过了，
                statsCommonDimension.setKpi(newInstallUserOfBrowserKpi);
                // 1. 
                statsUserDimension.setBrowser(br);
                // 2. 由于上面需要进行clean操作，故将该值进行clone后填充
                // statsUserDimension.setBrowser(WritableUtils.clone(br, context.getConfiguration()));

                /**
                 * 结果 statsUserDimension 中有common dimension(date,platdorm,kpi)，还有浏览器维度，
                 * timeOutputValue id为uuid，time为server time
                 * 这个写出是为浏览器信息分析中的新注册用户分析使用的，
                 */
                context.write(statsUserDimension, timeOutputValue);
            }
        }
    }
}
