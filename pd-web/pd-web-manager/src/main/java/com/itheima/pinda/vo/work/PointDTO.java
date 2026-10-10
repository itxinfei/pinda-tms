package com.itheima.pinda.vo.work;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "位置打点")
public class PointDTO {
    @Schema(description = "名称")
    private String name;
    @Schema(description = "坐标")
    private MarkerPoint markerPoint;

    public void setMarkerPoints(String lng, String lat) {
        MarkerPoint markerPoint = new MarkerPoint();
        markerPoint.setLat(lat);
        markerPoint.setLng(lng);
        this.markerPoint = markerPoint;
    }

    @Data
    class MarkerPoint {
        @Schema(description = "精度")
        private String lng;
        @Schema(description = "纬度")
        private String lat;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PointDTO) {
            PointDTO orderPointDTO = (PointDTO) obj;
            return (name.equals(orderPointDTO.name));
        }
        return super.equals(obj);
    }
}
