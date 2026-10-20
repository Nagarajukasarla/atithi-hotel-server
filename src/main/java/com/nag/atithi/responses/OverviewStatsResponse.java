package com.nag.atithi.responses;

public record OverviewStatsResponse(
    long todayCheckInCount,
    long todayCheckOutCount,
    long totalGuestsInHotel,
    long totalAvailableRooms
) {}
