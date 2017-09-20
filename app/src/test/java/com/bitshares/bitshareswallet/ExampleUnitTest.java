package com.bitshares.bitshareswallet;

import com.bitshares.bitshareswallet.wallet.account_object;
import com.bitshares.bitshareswallet.wallet.asset;
import com.bitshares.bitshareswallet.wallet.common.UnsignedShort;
import com.bitshares.bitshareswallet.wallet.fc.crypto.sha512_object;
import com.bitshares.bitshareswallet.wallet.fc.io.raw_type;
import com.bitshares.bitshareswallet.wallet.graphene.chain.asset_object;
import com.bitshares.bitshareswallet.wallet.graphene.chain.memo_data;
import com.bitshares.bitshareswallet.wallet.graphene.chain.object_id;
import com.bitshares.bitshareswallet.wallet.fc.crypto.sha256_object;
import com.bitshares.bitshareswallet.wallet.graphene.chain.operations;
import com.bitshares.bitshareswallet.wallet.graphene.chain.signed_transaction;
import com.bitshares.bitshareswallet.wallet.private_key;
import com.bitshares.bitshareswallet.wallet.public_key;
import com.bitshares.bitshareswallet.wallet.graphene.chain.types;
import com.bitshares.bitshareswallet.wallet.wallet_api;
import com.bitshares.bitshareswallet.wallet.websocket_api;
import com.google.common.io.BaseEncoding;
import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedLong;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.bitcoinj.core.ECKey;
import org.junit.Test;
import org.spongycastle.asn1.ASN1InputStream;
import org.spongycastle.asn1.ASN1Integer;
import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.crypto.digests.SHA256Digest;
import org.spongycastle.crypto.digests.SHA512Digest;
import org.spongycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.spongycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.openssl.PEMException;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.ECGenParameterSpec;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import de.bitsharesmunich.graphenej.BrainKey;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    public ExampleUnitTest() {
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
    }

    @Test
    public void pubic_key_type_test() throws NoSuchAlgorithmException {
        String strPulbicKey = "TEST6E5Yn1TZWQqB4pCstVQwZgE3oi6kzgWB7Uhu9oHy2JxNstS1CB";
        types.public_key_type public_key_type = new types.public_key_type(strPulbicKey);

        String strDestpublicKey = public_key_type.toString();
        assertEquals(strPulbicKey, strDestpublicKey);
    }

    @Test
    public void walletapi_key_generate_test() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDsA", "SC");
            ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256k1");
            keyGen.initialize(ecSpec, new SecureRandom());
            KeyPair keyPair = keyGen.generateKeyPair();

            BCECPrivateKey bcecPrivateKey = (BCECPrivateKey)keyPair.getPrivate();
            byte[] privateKeyGenerate = bcecPrivateKey.getD().toByteArray();

            byte[] keyData = new byte[32];
            if (privateKeyGenerate.length == 33) {
                System.arraycopy(privateKeyGenerate, 1, keyData, 0, keyData.length);
            } else {
                System.arraycopy(privateKeyGenerate, 0, keyData, 0, keyData.length);
            }

            private_key privateKey = new private_key(keyData);
            public_key publicKey = privateKey.get_public_key();
            byte bytePublic[] = publicKey.getKeyByte();

            BCECPublicKey bcecPublicKey = (BCECPublicKey) keyPair.getPublic();
            byte bytePublic1[] = bcecPublicKey.getQ().getEncoded(true);

            assertArrayEquals(bytePublic, bytePublic1);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void object_id_test() {
        String strMatch = "\\d+.\\d+.\\d+";
        String strAccountId = "1.2.7877877";
        assertTrue(strAccountId.matches(strMatch));

        strAccountId = "kfkjskfj";
        assertFalse(strAccountId.matches(strMatch));

        object_id<account_object>  testObject = new object_id<>(1, types.object_type.account_object_type, 4);
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(object_id.class, new object_id.object_id_deserializer());
        Gson gson = builder.create();
        object_id objectId = new object_id(1, types.object_type.account_object_type, 4);

        object_id<account_object> testObject3 = testObject.create_from_string("1.2.4");
        object_id<Integer> testObject4 = objectId.create_from_string("1.2.4");

        types.TestClass testClass = new types.TestClass();
        testClass.id = "1.2.4";

        String strResult = gson.toJson(testClass);

        types.TestClass2 testClass2 = gson.fromJson(strResult, types.TestClass2.class);


        String strResult2 = strResult;
    }

    @Test
    public void ripemd160_object_test() {
        String strContent = "00a0852a1508c982dbba2d77127e0b225a55a98a";

        // _hash = ([0] = 713400320, [1] = 2194212885, [2] = 1999485659, [3] = 571178514, [4] = 2326353242)


        int lHash2[] = new int[5];
        BaseEncoding encoding = BaseEncoding.base16().lowerCase();
        byte[] byteDecode = encoding.decode(strContent);

        int lValue = 0;
        lHash2[0] = ((byteDecode[3] & 0xff) << 24) | ((byteDecode[2] & 0xff) << 16) | ((byteDecode[1] & 0xff) << 8) | (byteDecode[0] & 0xff);
        lHash2[1] = ((byteDecode[7] & 0xff) << 24) | ((byteDecode[6] & 0xff) << 16) | ((byteDecode[5] & 0xff) << 8) | (byteDecode[4] & 0xff);
        lHash2[2] = ((byteDecode[11] & 0xff) << 24) | ((byteDecode[10] & 0xff) << 16) | ((byteDecode[9] & 0xff) << 8) | (byteDecode[8] & 0xff);
        lHash2[3] = ((byteDecode[15] & 0xff) << 24) | ((byteDecode[14] & 0xff) << 16) | ((byteDecode[13] & 0xff) << 8) | (byteDecode[12] & 0xff);
        lHash2[4] = ((byteDecode[19] & 0xff) << 24) | ((byteDecode[18] & 0xff) << 16) | ((byteDecode[17] & 0xff) << 8) | (byteDecode[16] & 0xff);

        int nValue = (byteDecode[1] & 0xff) << 8;

        char[] byteCotent = strContent.toCharArray();
        nValue = 713400320;
        String strValue = String.format(Locale.ENGLISH, "%x", nValue);

        int nTransfer = Integer.parseInt(strValue, 16);

        String strValue1 = Long.toHexString(nValue);

        int[] hash = new int[strContent.length() / 8];
        for (int i = 0; i < strContent.length() / 8; ++i) {
            String strHash = strContent.substring(i * 8, (i + 1) * 8);
            hash[i] = Integer.parseInt(strHash, 16);
        }

    }

    @Test
    public void private_key_sign_test() throws ParseException {
        signed_transaction signedTransaction = new signed_transaction();
        int nValue = 63152;
        signedTransaction.ref_block_num = new UnsignedShort((short)nValue);
        signedTransaction.ref_block_prefix = UnsignedInteger.valueOf(2647773290l);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        signedTransaction.expiration = simpleDateFormat.parse("2017-08-30T07:48:48");
        String strResultDate = simpleDateFormat.format(signedTransaction.expiration);
        signedTransaction.operations = new ArrayList<>();
        operations.operation_type operationType = new operations.operation_type();
        operationType.nOperationType = 0;

        operations.transfer_operation transferOperation = new operations.transfer_operation();
        transferOperation.from = new object_id<account_object>(1, 2, 3459);
        transferOperation.to = new object_id<account_object>(1, 2, 3529);
        transferOperation.amount = new asset(100000, new object_id<asset_object>(1, 3, 0));
        transferOperation.fee = new asset(100, new object_id<asset_object>(1, 3, 0));
        transferOperation.extensions = new HashSet<>();

        operationType.operationContent = transferOperation;

        signedTransaction.operations.add(operationType);
        signedTransaction.extensions = new HashSet<>();
    }
}