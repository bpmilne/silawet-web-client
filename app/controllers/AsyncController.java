package controllers;

import akka.actor.ActorSystem;
import javax.inject.*;

import com.fasterxml.jackson.databind.JsonNode;
import models.SilawetMessage;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import play.*;
import play.libs.Json;
import play.mvc.*;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;

import scala.concurrent.duration.Duration;
import scala.concurrent.ExecutionContextExecutor;

/**
 * This controller contains an action that demonstrates how to write
 * simple asynchronous code in a controller. It uses a timer to
 * asynchronously delay sending a response for 1 second.
 *
 * @param actorSystem We need the {@link ActorSystem}'s
 * {@link Scheduler} to run code after a delay.
 * @param exec We need a Java {@link Executor} to apply the result
 * of the {@link CompletableFuture} and a Scala
 * {@link ExecutionContext} so we can use the Akka {@link Scheduler}.
 * An {@link ExecutionContextExecutor} implements both interfaces.
 */
@Singleton
public class AsyncController extends Controller {

    private final ActorSystem actorSystem;
    private final ExecutionContextExecutor exec;

    @Inject
    public AsyncController(ActorSystem actorSystem, ExecutionContextExecutor exec) {
        this.actorSystem = actorSystem;
        this.exec = exec;
    }

    /**
     * An action that returns a plain text message after a delay
     * of 1 second.
     *
     * The configuration in the <code>routes</code> file means that this method
     * will be called when the application receives a <code>GET</code> request with
     * a path of <code>/message</code>.
     */
    public CompletionStage<Result> startPolling() {
        return startFuturePolling(1, TimeUnit.SECONDS).thenApplyAsync(Results::ok, exec);
    }

    private CompletionStage<String> startFuturePolling(long time, TimeUnit timeUnit) {
        CompletableFuture<String> future = new CompletableFuture<>();
        actorSystem.scheduler().schedule(
                Duration.create(time, timeUnit),
                Duration.create(1, TimeUnit.MINUTES),
                () -> {
                    try {
                        SilawetMessage message = SilawetMessage.findMostRecent();
                        String authored_at = "0";
                        if  (message != null)
                        {
                            authored_at = message.authored_at;
                        }
                        String response = "";
                        try
                        {
                            response = findMessageSince(authored_at);
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                        }

                        JsonNode result = Json.parse(response);
                        for (int i = 0; i < result.size(); i++) {
                            try {
                                JsonNode node = result.get(i);
                                SilawetMessage.create(node);
                            }
                            catch (Exception e)
                            {
//                                SilawetError.create(e.getMessage());
                            }
                        }
                    }
                    catch (Exception e)
                    {
//                        SilawetError.create(e.getMessage());
                    }
                },
                exec
        );
        future.complete("started");
        return future;
    }

    private String findMessageSince(String time) throws IOException {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet("http://api.silawet.com/Messages?since="+time);
        httpget.addHeader("Accept", "application/json");
        HttpResponse response = httpclient.execute(httpget);
        ResponseHandler<String> handler = new BasicResponseHandler();
        return handler.handleResponse(response);
    }
}
