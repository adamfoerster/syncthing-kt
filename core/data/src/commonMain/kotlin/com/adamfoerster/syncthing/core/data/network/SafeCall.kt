package com.adamfoerster.syncthing.core.data.network

import com.adamfoerster.syncthing.core.domain.util.DataError
import com.adamfoerster.syncthing.core.domain.util.Result
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

suspend inline fun <reified T> safeCall(execute: () -> HttpResponse): Result<T, DataError.Remote> {
    val response = try {
        execute()
    } catch (e: CancellationException) {
        throw e
    } catch (e: UnresolvedAddressException) {
        return Result.Failure(DataError.Remote.CORE_UNAVAILABLE)
    } catch (e: IOException) {
        return Result.Failure(DataError.Remote.CORE_UNAVAILABLE)
    } catch (e: Exception) {
        return Result.Failure(DataError.Remote.UNKNOWN)
    }
    return responseToResult(response)
}

suspend inline fun <reified T> responseToResult(response: HttpResponse): Result<T, DataError.Remote> =
    when (response.status.value) {
        in 200..299 -> try {
            if (T::class == Unit::class) Result.Success(Unit as T) else Result.Success(response.body<T>())
        } catch (e: NoTransformationFoundException) {
            Result.Failure(DataError.Remote.SERIALIZATION)
        } catch (e: SerializationException) {
            Result.Failure(DataError.Remote.SERIALIZATION)
        }
        401, 403 -> Result.Failure(DataError.Remote.UNAUTHORIZED)
        in 400..499 -> Result.Failure(DataError.Remote.BAD_REQUEST)
        in 500..599 -> Result.Failure(DataError.Remote.SERVER_ERROR)
        else -> Result.Failure(DataError.Remote.UNKNOWN)
    }
