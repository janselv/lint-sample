package me.jansv.lintsample

import me.jansv.internallib.NetworkTraceOp
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Tag

class SomeImpl : NetworkTraceOp {
    override val group: String
        get() = TODO("Not yet implemented")
    override val operationName: String
        get() = TODO("Not yet implemented")
}

interface RetrofitApi {
    @GET
    suspend fun callMissingTraceOp(url: String, @Tag tag: NetworkTraceOp): Response<String>
}
