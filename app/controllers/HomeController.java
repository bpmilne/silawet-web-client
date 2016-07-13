package controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import models.SilawetMessage;
import models.SilawetUser;
import org.apache.commons.lang3.SystemUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.bouncycastle.crypto.CryptoException;
import play.Logger;
import play.data.DynamicForm;
import play.data.Form;
import play.libs.Json;
import play.mvc.*;

import views.html.*;

import java.io.IOException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * This controller contains an action to handle HTTP requests
 * to the application's home page.
 */
public class HomeController extends Controller {

    /**
     * An action that renders an HTML page with a welcome message.
     * The configuration in the <code>routes</code> file means that
     * this method will be called when the application receives a
     * <code>GET</code> request with a path of <code>/</code>.
     */

    public Result index()
    {
        String username = session("connected");
        SilawetUser user = null;

        if (username != null)
        {
            user = SilawetUser.findByUsername(username);

        }

        List<SilawetMessage> messages = SilawetMessage.getPage(0);

        return ok(views.html.index.render(user, "", messages, 1, -1));
    }

    public Result page(int page) {

        String username = session("connected");
        SilawetUser user = null;

        if (username != null)
        {
            user = SilawetUser.findByUsername(username);

        }

        List<SilawetMessage> messages = SilawetMessage.getPage(page);

        return ok(views.html.index.render(user, "", messages, page+1, page-1));
    }

    public Result dashboard()
    {
        List<SilawetUser> rssList = SilawetUser.findAll();

        return ok(dashboard.render(rssList, false));
    }

    public Result profile()
    {
        String username = session("connected");
        SilawetUser user = null;

        if (username != null)
        {
            user = SilawetUser.findByUsername(username);
            return ok(profile.render(user));
        }
        return redirect("/dashboard");
    }

    public Result message(String messageId) {

        SilawetMessage silawetMessage = SilawetMessage.findBySilawetId(messageId);
        String username = session("connected");

        return ok(views.html.message.render(silawetMessage));
    }

    public Result author(String authorId)
    {
        List<SilawetMessage> silawetMessages = SilawetMessage.findByAuthorId(authorId);

        return ok(views.html.author.render(silawetMessages.get(0).authored_by, silawetMessages));
    }

    public Result delete(Long id)
    {
        SilawetUser rss = SilawetUser.findById(id);
        rss.delete();

        return redirect("/dashboard");
    }

    public Result regenerateKey()
    {
        String username = session("connected");
        SilawetUser user = null;

        if (username != null)
        {
            user = SilawetUser.findByUsername(username);
            user.regenerateKey();
            user.save();

            return ok(profile.render(user));
        }
        return redirect("/dashboard");
    }

    public Result register()
    {
        DynamicForm requestData = Form.form().bindFromRequest();
        String username = requestData.get("registerUsername");
        String password = requestData.get("registerPassword");

        SilawetUser user = SilawetUser.create(username, password);
        if (user != null)
        {
            session("connected", username);
            return redirect("/");
        }

        return redirect("/dashboard");
    }
    public Result login()
    {
        DynamicForm requestData = Form.form().bindFromRequest();
        String username = requestData.get("username");
        String password = requestData.get("password");

        SilawetUser user = SilawetUser.login(username, password);
        if (user != null) {
            session("connected", username);
            return redirect("/");
        }

        return redirect("/dashboard");
    }

    public Result logout()
    {
        session().remove("connected");
        session().clear();

        return redirect("/dashboard");
    }

    public Result delete()
    {
        String username = session("connected");

        if (username != null)
        {
            SilawetUser user = SilawetUser.findByUsername(username);
            user.delete();
        }
        return redirect("/dashboard");
    }

    public Result sendMessage()
    {
        DynamicForm requestData = Form.form().bindFromRequest();
        String message = requestData.get("message");
        message = message.replace("—","-");
        message = message.replace("’","'");
        message = message.replace("“","'");
        message = message.replace("”","'");
        message = message.replaceAll("[^a-zA-Z0-9&,\":'\\-\\}\\{ _]|^\\s+","");

        System.out.println(message);

        String username = session("connected");

        if (username != null)
        {
            SilawetUser user = SilawetUser.findByUsername(username);

            if (user != null) {
                SilawetMessage silawetMessage = SilawetMessage.create(message, user);
                try {
                    postToSilawet(silawetMessage);
                } catch (Exception e) {
                    System.out.println("exception " + e.getMessage());

                }
            }
        }

        return redirect("/");
    }

    private static void postToSilawet(SilawetMessage message) throws CryptoException, IOException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, InvalidKeySpecException {

        ObjectNode messageJson = Json.newObject();
        messageJson.put("message", message.message);
        String escapedMessage = messageJson.toString();

        long epoch = Long.parseLong(message.authored_at);

        String json = "{\"id\":\""+message.silawet_id+"\","+escapedMessage.substring(1, escapedMessage.length()-1)+",\"signature\":\""+message.signature+"\",\"authored_by\":\""+message.authored_by+"\",\"authored_at\":"+epoch+".3372}";
        StringEntity jsonBody = new StringEntity(json);

        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost("http://api.silawet.com/Messages");
        httpPost.setEntity(jsonBody);
        httpPost.addHeader("Accept", "application/json");
        httpPost.addHeader("Content-Type", "application/json");
        HttpResponse response = httpclient.execute(httpPost);

        int code = response.getStatusLine().getStatusCode();

        if (code == 200)
        {
            ResponseHandler<String> handler = new BasicResponseHandler();
            String responseBody = handler.handleResponse(response);
        }
        else if (code == 400)
        {
            System.out.println("didn't work ");
            System.out.println("json " + json);
            message.delete();
        }
    }
}
