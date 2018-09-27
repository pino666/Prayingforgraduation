package com.beifeng.transformer.model.value.map;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;

import com.beifeng.common.KpiType;
import com.beifeng.transformer.model.value.BaseStatsValueWritable;

/**
 * 定义一系列的字符串 mapper 输出类
 * 统计地域信息的mapper输出的时候，不光需要id和time，所以TimeOutputValue不再适用，所以定义此类，
 * 需要uuid 和 sid
 * 活跃用户：uuid的去重个数
 * 总会话数：u_sd的去重个数
 * 跳出会话个数：只访问一个pv的会话总个数
 *
 * @author gerry
 *
 */
public class TextsOutputValue extends BaseStatsValueWritable {
    private KpiType kpiType;
    private String uuid; // 用户唯一标识符
    private String sid; // 会话id

    public TextsOutputValue() {
        super();
    }

    public TextsOutputValue(String uuid, String sid) {
        super();
        this.uuid = uuid;
        this.sid = sid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public void setKpiType(KpiType kpiType) {
        this.kpiType = kpiType;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        this.internalWriteString(out, this.uuid);
        this.internalWriteString(out, this.sid);
    }

    private void internalWriteString(DataOutput out, String value) throws IOException {
        if (StringUtils.isEmpty(value)) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            out.writeUTF(value);
        }
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        this.uuid = this.internalReadString(in);
        this.sid = this.internalReadString(in);
    }

    private String internalReadString(DataInput in) throws IOException {
        return in.readBoolean() ? in.readUTF() : null;
    }

    @Override
    public KpiType getKpi() {
        return this.kpiType;
    }

}
