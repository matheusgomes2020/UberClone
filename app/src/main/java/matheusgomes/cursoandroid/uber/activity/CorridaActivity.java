package matheusgomes.cursoandroid.uber.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.navigation.ui.AppBarConfiguration;


import matheusgomes.cursoandroid.uber.R;
import matheusgomes.cursoandroid.uber.config.ConfiguracaoFirebase;
import matheusgomes.cursoandroid.uber.databinding.ActivityCorridaBinding;
import matheusgomes.cursoandroid.uber.helper.UsuarioFirebase;
import matheusgomes.cursoandroid.uber.model.Destino;
import matheusgomes.cursoandroid.uber.model.Requisicao;
import matheusgomes.cursoandroid.uber.model.Usuario;

public class CorridaActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    /*
    * Lat/lon destino: -22.87514269288448, -47.149738491208694 (R. Nossa Sra. da Conceição, 30)
    * Lat/lon passageiro: -22.876618029681712, -47.143291802153826
    * Lat/lon motorista (a caminho)
    *   inicial: -22.872710947018167, -47.14473885383925
    *   intermediaria: -22.87407510250004, -47.14630526383054
    *   final: -22.875775335077922, -47.14411658137696
    * Encerramento intermediário: -22.87414269288448, -47.149738491208694
    * Encerramento de corrida: -22.87514269288448, -47.149738491208694
    * */

    private GoogleMap mMap;

    private FirebaseAuth autenticacao;

    private LocationManager locationManager;

    private LocationListener locationListener;

    private LatLng localMotorista;

    private LatLng localpassageiro;

    private Requisicao requisicao;

    private Usuario motorista;

    private Usuario passageiro;

    private String idRequisicao;

    private DatabaseReference firbaseRef;

    private Marker marcadorMotorista;

    private Marker marcadorPassageiro;

    private Marker marcadorDestino;

    private String statusRequisicao;

    private Boolean requisicaoAtiva;

    private Destino destino;

    private AppBarConfiguration appBarConfiguration;
    private ActivityCorridaBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCorridaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setTitle("Iniciar corrida");

        firbaseRef = ConfiguracaoFirebase.getFirebaseDatabase();

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled( true );

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        binding.fabRota.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                String status = statusRequisicao;

                if ( status != null && !status.isEmpty() ){

                    String lat = "";
                    String lon = "";

                    switch ( status ){
                        case Requisicao.STATUS_A_CAMINHO :
                            lat = String.valueOf( localpassageiro.latitude );
                            lon = String.valueOf( localpassageiro.longitude );
                            break;
                        case Requisicao.STATUS_VIAGEM :
                            lat = destino.getLatitude();
                            lon = destino.getLongitude();
                            break;
                    }

                    //Abrir rota
                    String latLong = lat + "," + lon;
                    Toast.makeText(CorridaActivity.this, latLong, Toast.LENGTH_SHORT).show();
                    Uri uri = Uri.parse("google.navigation:q=" + latLong + "&mode=d");
                    Intent i = new Intent(Intent.ACTION_VIEW, uri);
                    i.setPackage("com.google.android.apps.maps");
                    startActivity( i );


                }

            }
        });

        //Recupera dados do usuário
        if( getIntent().getExtras().containsKey("idRequisicao")
                && getIntent().getExtras().containsKey("motorista") ){
            Bundle extras = getIntent().getExtras();
            motorista = (Usuario) extras.getSerializable("motorista");
            localMotorista = new LatLng(
                    Double.parseDouble( String.valueOf(0.0) ),
                    Double.parseDouble(String.valueOf(0.0) )
            );
            idRequisicao = extras.getString("idRequisicao");
            requisicaoAtiva = extras.getBoolean("requisicaoAtiva");
            verificaStatusRequisicao();
        }

    }

    private void verificaStatusRequisicao(){

        DatabaseReference requisicoes = firbaseRef.child("requisicoes")
                .child( idRequisicao );
        requisicoes.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                //Recupera requisição
                requisicao = dataSnapshot.getValue(Requisicao.class);
                if ( requisicao != null ){

                    passageiro = requisicao.getPassageiro();
                    localpassageiro = new LatLng(
                            Double.parseDouble(passageiro.getLatidude()),
                            Double.parseDouble(passageiro.getLongitude())
                    );

                    statusRequisicao = requisicao.getStatus();
                    destino = requisicao.getDestino();
                    alteraInterfaceStatusRequisicao( statusRequisicao );

                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


    }




    private void requisicaoAguardando(){
        binding.buttonAceitarCorrida.setText("Aceitar corrida");
        //Exibe marcador do motorista
        adicionaMarcadorMotorista(localMotorista, motorista.getNome() );

        mMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom( localMotorista, 20 ) );
    }

    private void requisicaoACaminho(){
        binding.buttonAceitarCorrida.setText("A caminho do passageiro");
        binding.fabRota.setVisibility( View.VISIBLE );

        //Exibe marcador do motorista
        adicionaMarcadorMotorista(localMotorista, motorista.getNome() );

        //Exibe marcador passageiro
        adicionaMarcadorPassageiro(localpassageiro, passageiro.getNome());

        //Centralizar dois marcadores
        centralizarDoisMarcadores(marcadorMotorista, marcadorPassageiro);

        //Inicia monitoramento do motorista / passageiro
        iniciarMonitoramento( motorista, localpassageiro, Requisicao.STATUS_VIAGEM );

    }

    private void requisicaoViagem(){

        //Altera interface
        binding.fabRota.setVisibility( View.VISIBLE );
        binding.buttonAceitarCorrida.setText( "A caminho do destino" );

        //Exibe marcador do motorista
        adicionaMarcadorMotorista( localMotorista, motorista.getNome() );

        //Exibe marcador de destino
        LatLng localDestino = new LatLng(
                Double.parseDouble( destino.getLatitude() ),
                Double.parseDouble( destino.getLongitude() )
        );
        adicionaMarcadorDestino( localDestino, "Destino" );

        //Centraliza marcadores motorista / destino
        centralizarDoisMarcadores( marcadorMotorista, marcadorDestino );

        //Inicia monitoramento do motorista / passageiro
        iniciarMonitoramento( motorista, localDestino, Requisicao.STATUS_FINALIZADA );



    }

    private void iniciarMonitoramento(Usuario uOrigem, LatLng localDestino, String status ){

        //Inicializar Geofire
        //Define nó de local de usuário
        DatabaseReference localUsuario = ConfiguracaoFirebase.getFirebaseDatabase()
                .child( "local_usuario" );

        GeoFire geoFire = new GeoFire( localUsuario );

        //Adiciona círculo no passageiro
        final Circle circulo = mMap.addCircle(
                new CircleOptions()
                        .center( localDestino )
                        .radius( 50 )//em metros
                        .fillColor(Color.argb( 90, 255, 153, 0 ) )
                        .strokeColor( Color.argb( 190, 255, 153, 0 ) )
        );

        final GeoQuery geoQuery = geoFire.queryAtLocation(
                new GeoLocation( localDestino.latitude, localDestino.longitude ),
                0.05//em km (0.05 50 metros)
        );

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {

                if ( key.equals( uOrigem.getId() ) ){
                    //Log.d( "onKeyEntered", "onKeyEntered: motorista está dentro da área!" );

                    //Altera status da requisicção
                    requisicao.setStatus( status );
                    requisicao.atualizarStatus();

                    geoQuery.removeAllListeners();
                    circulo.remove();

                }

            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

    }

    private void centralizarDoisMarcadores(Marker marcador1, Marker marcador2){

        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        builder.include( marcador1.getPosition() );
        builder.include( marcador2.getPosition() );

        LatLngBounds bounds = builder.build();

        int largura = getResources().getDisplayMetrics().widthPixels;
        int altura = getResources().getDisplayMetrics().heightPixels;
        int espacoInterno = (int) (largura * 0.20);

        mMap.moveCamera(
                CameraUpdateFactory.newLatLngBounds(bounds,largura,altura,espacoInterno)
        );

    }

    private void alteraInterfaceStatusRequisicao( String status ){

        switch ( status ){
            case Requisicao.STATUS_AGUARDANDO :
                requisicaoAguardando();
                break;
            case Requisicao.STATUS_A_CAMINHO :
                requisicaoACaminho();
                break;

            case Requisicao.STATUS_VIAGEM :
                requisicaoViagem();
                break;
        }

    }


    private void adicionaMarcadorMotorista(LatLng localizacao, String titulo){

        if( marcadorMotorista != null )
            marcadorMotorista.remove();

        marcadorMotorista = mMap.addMarker(
                new MarkerOptions()
                        .position(localizacao)
                        .title(titulo)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.carro))
        );

    }

    private void adicionaMarcadorPassageiro(LatLng localizacao, String titulo){

        if( marcadorPassageiro != null )
            marcadorPassageiro.remove();

        marcadorPassageiro = mMap.addMarker(
                new MarkerOptions()
                        .position(localizacao)
                        .title(titulo)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.usuario))
        );

    }

    private void adicionaMarcadorDestino(LatLng localizacao, String titulo){

        if( marcadorPassageiro != null ){
            marcadorPassageiro.remove();
        }

        if( marcadorDestino != null ){
            marcadorDestino.remove();
        }

        marcadorDestino = mMap.addMarker(
                new MarkerOptions()
                        .position(localizacao)
                        .title(titulo)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.destino))
        );

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

        //Recuperar localizacao do usuário
        recuperarLocalizacaoUsuario();

    }

    private void recuperarLocalizacaoUsuario() {

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                //recuperar latitude e longitude
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                localMotorista = new LatLng(latitude, longitude);

                //Atualizar Geofire
                UsuarioFirebase.atualizarDadosLocalizacao( latitude, longitude );

                //statusRequisicao = requisicao.getStatus();
                alteraInterfaceStatusRequisicao( statusRequisicao );

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        //Solicitar atualizações de localização
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ) {
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    10000,
                    10,
                    locationListener
            );
        }


    }

    public void aceitarCorrida( View view ){

        //Configura requisicao
        requisicao = new Requisicao();
        requisicao.setId( idRequisicao );
        requisicao.setMotorista( motorista );
        requisicao.setStatus( Requisicao.STATUS_A_CAMINHO );

        requisicao.atualizar();



    }

    @Override
    public boolean onSupportNavigateUp() {

        if ( requisicaoAtiva ){

            Toast.makeText(this, "Necessário encerrar a requisição atual!", Toast.LENGTH_SHORT).show();

        }else {

            Intent i = new Intent( CorridaActivity.this, RequisicoesActivity.class );
            startActivity( i );

        }

        return false;

    }
}