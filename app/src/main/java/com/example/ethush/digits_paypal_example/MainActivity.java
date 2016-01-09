package com.example.ethush.digits_paypal_example;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.digits.sdk.android.AuthCallback;
import com.digits.sdk.android.Digits;
import com.digits.sdk.android.DigitsAuthButton;
import com.digits.sdk.android.DigitsException;
import com.digits.sdk.android.DigitsSession;

import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;
import com.paypal.android.sdk.payments.PaymentConfirmation;

import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterCore;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;

import io.conekta.conektasdk.Card;
import io.conekta.conektasdk.Conekta;
import io.conekta.conektasdk.Token;

import io.fabric.sdk.android.Fabric;

public class MainActivity extends AppCompatActivity {

    // Note: Your consumer key and secret should be obfuscated in your source code before shipping.
    //Los KEY deben ser los generados en el panel de Fabric.io (-.-')
    private static final String TWITTER_KEY = "46jzF45XvDoOpEkAAd33O9A6I";
    private static final String TWITTER_SECRET ="xuH7Y2mcrIj5uRg0nqyUJniI1B4GKTFxC9UJByTg8rDLzKk5Qq05025";

    //Paypal set Up
    private static PayPalConfiguration payPalConfiguration = new PayPalConfiguration()
            .environment(PayPalConfiguration.ENVIRONMENT_NO_NETWORK)
            .clientId("AZdodt3GYKvEkVvbxAXyFvCGbMW2dHVQZDIJJObQtVvXMwfaPbUOxhjaXxhHMDDwGGJWZEWG_Ko_lt7a");


    //Conekta basics
    private Button btnTokenize;
    private Card conektaCard;
    private Token conektaToken;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //SetUp de las claves para auth de digits para numeros telefónicos
        TwitterAuthConfig authConfig = new TwitterAuthConfig(TWITTER_KEY, TWITTER_SECRET);
        Fabric.with(this, new TwitterCore(authConfig), new Digits());

        setContentView(R.layout.activity_main);

        //SetUp de Conekta para tokenizar tarjetas
        Conekta.setPublicKey("key_Hor2Y2c5sUo8t8YgAG8erNQ");
        Conekta.setApiVersion("0.3.0");
        Conekta.collectDevice(this);

        /*
        * Sección de integración basica de digits login
        * */
        DigitsAuthButton digitsButton = (DigitsAuthButton) findViewById(R.id.auth_button);
        digitsButton.setCallback(new AuthCallback() {
            @Override
            public void success(DigitsSession session, String phoneNumber) {
                //Se obtiene el numero de telefono y de la sesion si es un usuario valido con el hash para almacenarlo
                Toast.makeText(getApplicationContext(), "Login exitoso para el numero: "
                        + phoneNumber, Toast.LENGTH_LONG).show();
            }

            @Override
            public void failure(DigitsException exception) {
                Log.d("Digits", "Algo ha salido mal al procesar la solicitud", exception);
            }
        });



        /*
        * Sección de integración basica con funcionalidades añadidas en las funciones
        * -onDestroy
        * -OnBuyPressed
        * -onActivityResult
        * */
        Intent paypalIntent = new Intent(this, PayPalService.class);
        paypalIntent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, payPalConfiguration);

        startService(paypalIntent);



        /*
        * Integración de Conekta SDK para generar tokens de compra vía tarjeta de cŕedito
        * */
        btnTokenize = (Button) findViewById(R.id.btnTokenize);

        btnTokenize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Datos de ejemplo, el token es mostrado en un Toast y en el Log
                conektaCard = new Card("Jhon Doe Tester","371449635398431","123","01","2016");
                conektaToken = new Token(MainActivity.this);

                //Crea el token para agregar el cargo
                conektaToken.onCreateTokenListener(new Token.CreateToken() {
                    @Override
                    public void onCreateTokenReady(JSONObject data) {
                        try {
                            Log.d("CONEKTA INFO", data.getString("id"));

                            Toast.makeText(MainActivity.this, data.getString("id"), Toast.LENGTH_SHORT).show();

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        Log.d("CONEKTA INFO", "Device fingerprint is :: " +Conekta.deviceFingerPrint(MainActivity.this));
                    }
                });
                //Crea el token
                conektaToken.create(conektaCard);

            }
        });


    }

    @Override
    protected void onDestroy() {
        //al cerrar la ventana se detiene el activity de paypal si estuviera en proceso
        stopService(new Intent(this, PayPalService.class));
        super.onDestroy();
    }

    public void onBuyPressed(View Pressed) {

        //este es el pago
        PayPalPayment payment = new PayPalPayment(new BigDecimal("1.75"), "USD", "sample item",
                PayPalPayment.PAYMENT_INTENT_SALE);

        //Se  inicia el proceso y se le pasa el parametro de pago
        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, payPalConfiguration);
        intent.putExtra(PaymentActivity.EXTRA_PAYMENT, payment);

        //Se inicia y se queda a la espera de que la actividad de paypal termine
        startActivityForResult(intent, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //Valida si la actividad iniciada por PayPal finalizó correctamente
        if (resultCode == Activity.RESULT_OK) {
            PaymentConfirmation confirm = data.getParcelableExtra(PaymentActivity.EXTRA_RESULT_CONFIRMATION);
            if (confirm != null) {
                try {
                    //Si el pago es exitoso parseamos el JSON devuelto por paypal
                    Log.d("paymentExample", confirm.toJSONObject().toString(4));
                    //Obtenemos el ID de pago y el UniqueID para referencias
                    String response = confirm.toJSONObject().getJSONObject("response").getString("id");
                    Log.d("Unique ID", response);

                } catch (JSONException e) {
                    Log.d("paymentExample", "Ha ocurrido un error: ", e);
                }
            }
        }
        else if (resultCode == Activity.RESULT_CANCELED) {
            Log.d("paymentExample", "Compra cancelada.");
        }
        else if (resultCode == PaymentActivity.RESULT_EXTRAS_INVALID) {
            Log.d("paymentExample", "Pago ó configuración invalida.");
        }
    }
}
