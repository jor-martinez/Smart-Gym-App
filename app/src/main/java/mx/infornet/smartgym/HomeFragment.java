package mx.infornet.smartgym;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class HomeFragment extends Fragment {

    private View myView;
    private String nombreUsuario, token, token_type, nombre_gym, apellidos, objetivo;
    private TextView txt_nombre, txt_gym_valor, txt_dias_restantes, txt_status, status_color;
    private RelativeLayout btn_rutinas, btn_alim, btn_progreso;
    private StringRequest request;
    private RequestQueue queue;
    private int channel_id = 100;
    private NotificationCompat.Builder mBuilder;

    private Context context;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        myView = inflater.inflate(R.layout.fragment_home, container, false);

        txt_nombre = myView.findViewById(R.id.txt_nombre);
        txt_gym_valor = myView.findViewById(R.id.txt_gym_value);
        txt_dias_restantes = myView.findViewById(R.id.txt_until_value);
        txt_status = myView.findViewById(R.id.txt_status_value);
        status_color = myView.findViewById(R.id.status_color);
        btn_rutinas = myView.findViewById(R.id.btn_to_rutinas);
        btn_alim = myView.findViewById(R.id.btn_to_alimentacion);
        btn_progreso = myView.findViewById(R.id.btn_to_progreso);

        queue = Volley.newRequestQueue(getContext());

        ConexionSQLiteHelper conn = new ConexionSQLiteHelper(getActivity(), "usuarios", null, 4);
        SQLiteDatabase db = conn.getWritableDatabase();

        try {

            String query = "SELECT * FROM usuarios";
            //String imagenUsuario = null;

            Cursor cursor = db.rawQuery(query, null);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {

                nombreUsuario = cursor.getString(cursor.getColumnIndex("nombre"));
                apellidos = cursor.getString(cursor.getColumnIndex("apat"));
                token = cursor.getString(cursor.getColumnIndex("token"));
                token_type = cursor.getString(cursor.getColumnIndex("tokenType"));
                objetivo = cursor.getString(cursor.getColumnIndex("objetivo"));
            }

        } catch (Exception e) {

            Toast toast = Toast.makeText(getActivity(), "Error: " + e.toString(), Toast.LENGTH_SHORT);
            toast.show();
        }

        db.close();


        btn_rutinas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentrutinas = new Intent(getContext(), RutinasActivity.class);
                startActivity(intentrutinas);
            }
        });
        btn_alim.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentAlim = new Intent(getContext(), AlimentacionActivity.class);
                startActivity(intentAlim);
            }
        });
        btn_progreso.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (objetivo.equals("Perder peso")){
                    startActivity(new Intent(getContext(), ProgresoPerderPesoActivity.class));
                }

            }
        });


        request = new StringRequest(Request.Method.GET, Config.GET_INFO_GYM_URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonObject = new JSONObject(response);

                    //Log.e("RESPONSE_GYM", jsonObject.toString());

                    if (jsonObject.has("status")){

                        String status = jsonObject.getString("status");

                        if (status.equals("Token is Expired")){

                            Toast.makeText(getContext(), "Token expirado. Favor de iniciar sesión nuevamente", Toast.LENGTH_LONG).show();
                            ConexionSQLiteHelper  conn = new ConexionSQLiteHelper(getContext(), "usuarios", null, 4);
                            SQLiteDatabase db = conn.getWritableDatabase();
                            db.execSQL("DELETE FROM usuarios");

                            startActivity(new Intent(getContext(), LoginActivity.class));
                            getActivity().finish();

                        } else {


                        }

                    } else {
                        nombre_gym = jsonObject.getString("nombre");

                        txt_gym_valor.setText(nombre_gym);
                    }



                } catch (JSONException e) {
                    Log.e("ERROR_JSON", e.toString());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("ERROR_RESPONSE", error.toString());
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Authorization", token_type + " " + token);
                return headers;
            }
        };

        queue.add(request);

        String nomCompleto = nombreUsuario + " " + apellidos;
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        Date fecha_final = new Date();
        try {
            fecha_final = sdf.parse("07/12/2019");
        } catch (ParseException e) {
            e.printStackTrace();
        }

        long dias = getDiasRestantes(new Date(), fecha_final ) + 1;
        String dias_res = dias+" días";

//        Intent notifyIntent = new Intent(getContext(), BroadcastReceiver.class);
//        PendingIntent pendingIntent = PendingIntent.getBroadcast(getContext(), 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (dias > 0){
            txt_status.setText("Activo");
            status_color.setBackgroundColor(getResources().getColor(R.color.usuario_activo));

        } else{
            txt_status.setText("Inactivo");
            status_color.setBackgroundColor(getResources().getColor(R.color.design_default_color_error));
        }

        //Log.e("DIAS_REST", dias);
        txt_nombre.setText(nomCompleto);
        txt_dias_restantes.setText(dias_res);


        return myView;
    }

    private long getDiasRestantes(Date fecha_inicial, Date fecha_final){
        long diferencia = fecha_final.getTime() - fecha_inicial.getTime();

        //Log.i("MainActivity", "fechaInicial : " + fecha_inicial);
        //Log.i("MainActivity", "fechaFinal : " + fecha_final);

        long segsMilli = 1000;
        long minsMilli = segsMilli * 60;
        long horasMilli = minsMilli * 60;
        long diasMilli = horasMilli * 24;

        long diasTranscurridos = diferencia / diasMilli;
        diferencia = diferencia % diasMilli;

        long horasTranscurridos = diferencia / horasMilli;
        diferencia = diferencia % horasMilli;

        long minutosTranscurridos = diferencia / minsMilli;
        diferencia = diferencia % minsMilli;

        long segsTranscurridos = diferencia / segsMilli;

        return diasTranscurridos;
    }

}
