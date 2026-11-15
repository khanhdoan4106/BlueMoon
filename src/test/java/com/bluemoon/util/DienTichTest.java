package com.bluemoon.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Diện tích hiện một kiểu ở mọi màn (rà soát giao diện, mục T1). */
class DienTichTest {

    @Test
    void luonHaiChuSoLeDauPhayKemDonVi() {
        assertEquals("75,50 m²", DienTich.hien(new BigDecimal("75.5")));
        assertEquals("75,50 m²", DienTich.hien(new BigDecimal("75.50")));
        assertEquals("57,62 m²", DienTich.hien(new BigDecimal("57.62")));
        assertEquals("92,00 m²", DienTich.hien(new BigDecimal("92")));
        assertEquals("1.250,00 m²", DienTich.hien(new BigDecimal("1250")));
    }

    @Test
    void chiConSoDeDienVaoONhap() {
        assertEquals("75,50", DienTich.so(new BigDecimal("75.5")));
        assertEquals("450,00", DienTich.so(new BigDecimal("450")));
    }

    @Test
    void nullLaRong() {
        assertEquals("", DienTich.hien(null));
        assertEquals("", DienTich.so(null));
    }
}
