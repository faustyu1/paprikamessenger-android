package ru.faustyu.paprika.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.Signature
import android.util.Base64

/**
 * Manages cryptographic keys for Paprika:
 *
 * 1. Auth key (ECDSA P-256 in Android Keystore):
 *    - Private key never leaves the hardware chip
 *    - Used to sign challenges for passwordless login
 *    - Alias: KEYSTORE_AUTH_ALIAS
 *
 * 2. Legacy DH keys (kept for E2E encryption compatibility):
 *    - Used to compute shared secrets for E2E message encryption
 */
object CryptoManager {

    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    const val KEYSTORE_AUTH_ALIAS = "paprika_auth_key"

    // ── Auth key (Android Keystore, ECDSA P-256) ──────────────────────────

    /**
     * Generates an ECDSA P-256 key pair in Android Keystore.
     * Safe to call multiple times — does nothing if alias already exists.
     */
    fun generateAuthKeyIfNeeded() {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        if (keyStore.containsAlias(KEYSTORE_AUTH_ALIAS)) return

        val spec = KeyGenParameterSpec.Builder(
            KEYSTORE_AUTH_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setAlgorithmParameterSpec(java.security.spec.ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setUserAuthenticationRequired(false)
            .build()

        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KEYSTORE_PROVIDER).apply {
            initialize(spec)
            generateKeyPair()
        }
    }

    /**
     * Returns the ECDSA P-256 public key as base64-encoded PKIX/DER bytes.
     * This is what gets stored in the server as the user's public_key.
     * Returns null if the key hasn't been generated yet.
     */
    fun getPublicKeyBase64(): String? {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val cert = keyStore.getCertificate(KEYSTORE_AUTH_ALIAS) ?: return null
        return Base64.encodeToString(cert.publicKey.encoded, Base64.NO_WRAP)
    }

    /**
     * Signs challengeHex (32-byte challenge as hex string) using SHA256withECDSA.
     * Returns a base64-encoded DER-encoded ASN.1 signature.
     * Matches what the server verifies with crypto/ecdsa.VerifyASN1.
     */
    fun signChallenge(challengeHex: String): String {
        val challengeBytes = challengeHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val privateKey = keyStore.getKey(KEYSTORE_AUTH_ALIAS, null)
            ?: throw IllegalStateException("Auth key not found in Keystore")

        val sig = Signature.getInstance("SHA256withECDSA").apply {
            initSign(privateKey)
            update(challengeBytes)
        }.sign()

        return Base64.encodeToString(sig, Base64.NO_WRAP)
    }

    /**
     * Returns true if the auth key exists in Android Keystore.
     */
    fun hasAuthKey(): Boolean {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        return keyStore.containsAlias(KEYSTORE_AUTH_ALIAS)
    }

    // ── Legacy DH keys (E2E encryption) ──────────────────────────────────

    private val P = BigInteger("FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7EDEE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3DC2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F83655D23DCA3AD961C62F356208552BB9ED529077096966D670C354E4ABC9804F1746C08CA18217C32905E462E36CE3BE39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9DE2BCBF6955817183995497CEA956AE515D2261898FA051015728E5A8AACAA68FFFFFFFFFFFFFFFF", 16)
    private val G = BigInteger("2")

    private var dhPrivateKey: BigInteger? = null
    var dhPublicKey: BigInteger? = null
        private set

    fun generateDHKeys() {
        val random = SecureRandom()
        dhPrivateKey = BigInteger(2048, random).mod(P)
        dhPublicKey = G.modPow(dhPrivateKey, P)
    }

    fun computeSharedSecret(serverPublicKeyStr: String): BigInteger {
        val serverPublicKey = BigInteger(serverPublicKeyStr, 16)
        if (dhPrivateKey == null) throw IllegalStateException("DH keys not generated")
        return serverPublicKey.modPow(dhPrivateKey, P)
    }
}
