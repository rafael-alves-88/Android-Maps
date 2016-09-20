package com.example.rm40300.metromapas;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.example.rm40300.metromapas.model.Estacao;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        if (ContextCompat.checkSelfPermission(MapsActivity.this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startMap();
        } else {
            ActivityCompat.requestPermissions(MapsActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    0);
        }
    }

    private void startMap() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case 0:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startMap();
                } else {
                    Toast.makeText(MapsActivity.this, "No permission to access location!", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng fiap = new LatLng(-23.5729661, -46.6228372);
        //mMap.addMarker(new MarkerOptions().position(fiap).title("Fiap").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher)));
        mMap.addMarker(new MarkerOptions().position(fiap).title("Fiap"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(fiap, 17.0f));

        BuscarCoordenadas buscarCoordenadas = new BuscarCoordenadas();
        buscarCoordenadas.execute("http://www.heiderlopes.com.br/metro/estacoesmetro.json");
    }

    private class BuscarCoordenadas extends AsyncTask<String, Void, List<Estacao>> {

        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            //super.onPreExecute();
            progressDialog = ProgressDialog.show(MapsActivity.this, "Carregando...", "Buscando os dados");
        }

        @Override
        protected List<Estacao> doInBackground(String... params) {
            List<Estacao> estacoes = new ArrayList<>();
            try {
                URL url = new URL(params[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/json");

                if (connection.getResponseCode() == 200) {
                    BufferedReader stream = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));

                    String linha = "";
                    StringBuilder retorno = new StringBuilder();

                    while ((linha = stream.readLine()) != null) {
                        retorno.append(linha);
                    }
                    connection.disconnect();

                    JSONArray array = new JSONArray(retorno.toString());
                    for (int i = 0; i < array.length(); i++) {
                        Estacao estacao = new Estacao();
                        estacao.setLatitude(array.getJSONObject(i).getString("latitude"));
                        estacao.setLongitude(array.getJSONObject(i).getString("longitude"));
                        estacao.setLinha(array.getJSONObject(i).getString("linha"));
                        estacao.setNome(array.getJSONObject(i).getString("estacao"));

                        estacoes.add(estacao);
                    }

                    return estacoes;
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(List<Estacao> estacoes) {
            //super.onPostExecute(s);
            if (estacoes != null && estacoes.size() > 0) {
                colocarNoMapa(estacoes);
            }
            progressDialog.dismiss();
        }

        private void colocarNoMapa(List<Estacao> estacoes) {
            for (Estacao estacao : estacoes) {
                LatLng posicao = new LatLng(Double.parseDouble(estacao.getLatitude()), Double.parseDouble(estacao.getLongitude()));
                mMap.addMarker(new MarkerOptions().position(posicao).title(estacao.getNome()).snippet(String.format("Linha %s", estacao.getLinha())));
            }
        }
    }
}