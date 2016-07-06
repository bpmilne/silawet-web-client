package models;

import com.avaje.ebean.Model;
import org.bouncycastle.crypto.KeyGenerationParameters;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PEMWriter;
import play.data.format.Formats;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * Created by nickschulze on 4/15/16.
 */
@Entity
public class SilawetUser extends Model{

    @Id
    @GeneratedValue
    public Long id;
    public String username;
    public String password;
    public String private_key;
    public String public_key;
    @Formats.DateTime(pattern="dd/MM/yyyy HH:mm:ss")
    public Date created_date;
    public boolean hidden;

    public static Finder<Long, SilawetUser> find = new Finder<Long,SilawetUser>(SilawetUser.class);

    public SilawetUser(String _username, String _password)
    {
        username = _username;
        SecureRandom randomizer = new SecureRandom();
        randomizer.generateSeed(24);
        String salt = String.valueOf(randomizer.nextInt());
        password = hashWithSalt(_password, salt);

        try
        {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair keys = keyGen.genKeyPair();

            StringWriter privateWriter = new StringWriter();
            PEMWriter privatePEMWriter = new PEMWriter(privateWriter);
            try
            {
                privatePEMWriter.writeObject(keys.getPrivate());
                privatePEMWriter.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            private_key = privateWriter.toString();

            StringWriter publicWriter = new StringWriter();
            PEMWriter publicPEMWriter = new PEMWriter(publicWriter);
            try
            {
                publicPEMWriter.writeObject(keys.getPublic());
                publicPEMWriter.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            public_key = publicWriter.toString();
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }

        hidden = false;
        created_date = new Date();
    }

    public static SilawetUser create(String _username, String _password)
    {
        SilawetUser silawetUser = null;
        if(SilawetUser.findByUsername(_username) == null) {
            System.out.println("register");
            silawetUser = new SilawetUser(_username, _password);
            silawetUser.save();
        }

        return silawetUser;
    }

    public static SilawetUser findById(Long id)
    {
        return find.where().eq("id", id).findUnique();
    }

    public static List<SilawetUser> findAll()
    {
        return find.all();
    }

    public static SilawetUser findByUsername(String username) {
        return find.where().eq("username", username).findUnique();
    }

    public static SilawetUser login(String username, String password)
    {
        SilawetUser user = find.where()
                .eq("username", username)
                .findUnique();
        boolean validated;
        if(user != null)
        {
            try
            {
                validated = validatePassword(password.toCharArray(), user.password);
            }
            catch (NoSuchAlgorithmException e)
            {
                validated = false;
                e.printStackTrace();
            }
            catch (InvalidKeySpecException e)
            {
                validated = false;
                e.printStackTrace();
            }
            if(validated)
            {
                return user;
            }
        }

        return null;    }

    private static String hashWithSalt(String password, String salt)
    {
        byte[] hash = new byte[0];
        try
        {
            hash = pbkdf2(password.toCharArray(), salt.getBytes(), 1000, 24);
        }
        catch (NoSuchAlgorithmException e)
        {
            //not sure how to handle
            //TODO
            e.printStackTrace();
        }
        catch (InvalidKeySpecException e)
        {
            //not sure how to handle
            //TODO
            e.printStackTrace();
        }
        SecureRandom randomizer = new SecureRandom();
        String rando = String.valueOf(randomizer.nextInt());

        return String.valueOf(rando+":"+toHex(salt.getBytes())+":"+toHex(hash));
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int bytes) throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, bytes * 8);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        return skf.generateSecret(spec).getEncoded();
    }

    public static boolean validatePassword(char[] password, String correctHash)
            throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        // Decode the hash into its parameters
        String[] params = correctHash.split(":");
        int iterations = Integer.parseInt(params[0]);
        byte[] salt = fromHex(params[1]);
        byte[] hash = fromHex(params[2]);
        // Compute the hash of the provided password,
        // Compute the hash of the provided password, using the same salt,
        // iteration count, and hash length
        byte[] testHash = pbkdf2(password, salt, 1000, hash.length);
        // Compare the hashes in constant time. The password is correct if
        // both hashes match.
        return slowEquals(hash, testHash);
    }

    private static boolean slowEquals(byte[] a, byte[] b)
    {
        int diff = a.length ^ b.length;
        for(int i = 0; i < a.length && i < b.length; i++)
            diff |= a[i] ^ b[i];
        return diff == 0;
    }

    private static String toHex(byte[] array)
    {
        BigInteger bi = new BigInteger(1, array);
        String hex = bi.toString(16);
        int paddingLength = (array.length * 2) - hex.length();
        if(paddingLength > 0)
            return String.format("%0" + paddingLength + "d", 0) + hex;
        else
            return hex;
    }

    private static byte[] fromHex(String hex)
    {
        byte[] binary = new byte[hex.length() / 2];
        for(int i = 0; i < binary.length; i++)
        {
            binary[i] = (byte)Integer.parseInt(hex.substring(2*i, 2*i+2), 16);
        }
        return binary;
    }

    public void regenerateKey()
    {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair keys = keyGen.genKeyPair();

            StringWriter privateWriter = new StringWriter();
            PEMWriter privatePEMWriter = new PEMWriter(privateWriter);
            try {
                privatePEMWriter.writeObject(keys.getPrivate());
                privatePEMWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            private_key = privateWriter.toString();

            StringWriter publicWriter = new StringWriter();
            PEMWriter publicPEMWriter = new PEMWriter(publicWriter);
            try {
                publicPEMWriter.writeObject(keys.getPublic());
                publicPEMWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            public_key = publicWriter.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();

        }
    }
}

