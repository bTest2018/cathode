package net.simonvt.trakt.api.service;

import retrofit.http.GET;

import net.simonvt.trakt.api.entity.ServerTime;

public interface ServerService {

    @GET("/server/time.json/{apikey}")
    ServerTime time();
}
