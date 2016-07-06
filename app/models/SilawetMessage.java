package models;

import com.avaje.ebean.Model;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.openssl.PEMReader;
import play.data.format.Formats;
import play.libs.Json;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.io.*;
import java.nio.charset.Charset;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by nickschulze on 4/15/16.
 */
@Entity
public class SilawetMessage extends Model{

    @Id
    @GeneratedValue
    public Long id;
    public String silawet_id;
    public String message;
    public String signature;
    public String authored_by;
    public String authored_at;
    @Formats.DateTime(pattern="dd/MM/yyyy HH:mm:ss")
    public Date created_at;
    public int year;
    public int month;
    public int day;
    public int hour;

    public static Finder<Long, SilawetMessage> find = new Finder<Long,SilawetMessage>(SilawetMessage.class);

    public SilawetMessage(String _silawetId, String _message, String _signature, String _authored_by, String _authored_at, Date _created_at)
    {
        silawet_id = _silawetId;
        message = _message;
        signature = _signature;
        authored_by = _authored_by;
        authored_at = _authored_at;
        created_at = _created_at;
        Calendar cal = Calendar.getInstance();
        cal.setTime(created_at);
        year = cal.get(Calendar.YEAR);
        month = cal.get(Calendar.MONTH);
        day = cal.get(Calendar.DAY_OF_MONTH);
        hour = cal.get(Calendar.HOUR_OF_DAY);
    }

    public static SilawetMessage create(String message, SilawetUser user)
    {
            String silawet_id = java.util.UUID.randomUUID().toString();

            Date today = new Date();
            long epoch = today.getTime();
            String authored_at = Long.toString(epoch);

            ObjectNode messageJson = Json.newObject();
            messageJson.put("message", message);

            try {

                String signature = rsaSign(silawet_id, messageJson, epoch, user.private_key);

                String authored_by = authoredBy(user.public_key);

                SilawetMessage silawetMessage = new SilawetMessage(silawet_id, message, signature, authored_by, authored_at, new Date());
                silawetMessage.save();

                return silawetMessage;

            } catch (Exception e) {

                return null;
            }
    }

    public static List<SilawetMessage> getPage(int page)
    {
        return find.where()
                .gt("year", 2015)
                .orderBy("created_at desc")
                .findPagedList(page, 10)
                .getList();
    }

    public static SilawetMessage findMostRecent()
    {
        List<SilawetMessage> messages = find.where().gt("year",2015).orderBy("created_at desc").findPagedList(0, 10).getList();
        if  (messages.size() > 0)
        {
            return messages.get(0);
        }
        else
        {
            return null;
        }
    }

    public static SilawetMessage findById(Long id)
    {
        return find.where().eq("id", id).findUnique();
    }

    public static List<SilawetMessage> findAll()
    {
        return find.all();
    }


    public static SilawetMessage findBySilawetId(String messageId) {

        try
        {
            return find.where().eq("silawet_id", messageId).findUnique();
        }
        catch (Exception e)
        {
            System.out.println(e.toString());
            List<SilawetMessage> messageList = find.where().eq("silawet_id", messageId).findList();
            for (int i = 1;  i <messageList.size(); i++) {
                messageList.get(i).delete();
            }
            return messageList.get(0);
        }
    }

    public static SilawetMessage create(JsonNode node)
    {
        String _silawet_id = String.valueOf(node.get("id"));
        _silawet_id = _silawet_id.substring(1, _silawet_id.length()-1);

        if (findBySilawetId(_silawet_id) == null) {
            String _message = String.valueOf(node.get("message"));
            _message = _message.substring(1, _message.length() - 1);

            String _signature = String.valueOf(node.get("signature"));
            _signature = _signature.substring(1, _signature.length() - 1);

            String _authored_by = String.valueOf(node.get("authored_by"));
            _authored_by = _authored_by.substring(1, _authored_by.length() - 1);

            String _authored_at = String.valueOf(node.get("authored_at"));
            Long _authored_at_long = parseExponentialLong(_authored_at);
            Date _createdDate = new Date(_authored_at_long);

            SilawetMessage silawetMessage = new SilawetMessage(_silawet_id, _message, _signature, _authored_by, _authored_at_long.toString(), _createdDate);
            silawetMessage.save();

            return silawetMessage;
        }
        return null;
    }

    private static long parseExponentialLong(String authored_at)
    {
        Double number = Double.parseDouble(authored_at);
        String numberStr = String.format("%.0f", number);

        return Long.parseLong(numberStr);
    }

    private static String rsaSign(String silawet_id, ObjectNode message, long epoch, String privateKeyString) throws CryptoException, IOException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, InvalidKeySpecException {

        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        StringReader sr = new StringReader(privateKeyString);

        PEMReader pemReader = new PEMReader(sr);

        KeyPair keyPair = (KeyPair) pemReader.readObject();
        PrivateKey key = keyPair.getPrivate();

        Signature signature = Signature.getInstance("MD5withRSA");
        signature.initSign(key);

        String escapedMessage = message.toString();
        String json = "{\"id\":\""+silawet_id+"\","+escapedMessage.substring(1, escapedMessage.length()-1)+",\"authored_at\":"+epoch+".3372}";

        byte[] data = json.getBytes("UTF-8");
        signature.update(data, 0, data.length);

        byte [] signed = signature.sign();

        byte[] byteArray = Base64.getEncoder().encode(signed);

        return new String(byteArray);
    }

    private static String authoredBy(String publicKeyString) throws UnsupportedEncodingException {
        byte[] publicBytes = publicKeyString.getBytes("UTF-8");
        byte[] byteArray = Base64.getEncoder().encode(publicBytes);

        return new String(byteArray);
    }
}

