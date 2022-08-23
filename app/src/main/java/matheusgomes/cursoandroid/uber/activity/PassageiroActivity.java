package matheusgomes.cursoandroid.uber.activity;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.navigation.ui.AppBarConfiguration;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import matheusgomes.cursoandroid.uber.R;
import matheusgomes.cursoandroid.uber.config.ConfiguracaoFirebase;
import matheusgomes.cursoandroid.uber.databinding.ActivityPassageiroBinding;
import matheusgomes.cursoandroid.uber.helper.Local;
import matheusgomes.cursoandroid.uber.helper.UsuarioFirebase;
import matheusgomes.cursoandroid.uber.model.Destino;
import matheusgomes.cursoandroid.uber.model.Requisicao;
import matheusgomes.cursoandroid.uber.model.Usuario;

public class PassageiroActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FirebaseAuth autenticacao;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private LatLng localPassageiro;
    private LatLng localMotorista;
    private Boolean cancelarUber = false;
    private DatabaseReference firbaseRef;
    private Requisicao requisicao;
    private AppBarConfiguration appBarConfiguration;
    private ActivityPassageiroBinding binding;
    private Usuario passageiro;
    private Usuario motorista;
    private String statusRequisicao;
    private Destino destino;
    private Marker marcadorMotorista;
    private Marker marcadorPassageiro;
    private Marker marcadorDestino;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityPassageiroBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setTitle("Iniciar uma viagem");

        setSupportActionBar(binding.toolbar);

        //Configurações iniciais
        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
        firbaseRef = ConfiguracaoFirebase.getFirebaseDatabase();

        //Adiciona listener para status da requisição
        verificaStatusRequisicao();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    private void verificaStatusRequisicao() {

        Usuario usuarioLogado = UsuarioFirebase.getDadosUsuarioLogado();
        DatabaseReference requisicoes = firbaseRef.child( "requisicoes" );
        Query requisicaoPesquisa = requisicoes.orderByChild( "passageiro/id" )
                .equalTo( usuarioLogado.getId() );

        requisicaoPesquisa.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                List<Requisicao> lista = new ArrayList<>();
                for ( DataSnapshot ds : snapshot.getChildren() ){

                    requisicao = ds.getValue( Requisicao.class );
                    lista.add( requisicao );
                }

                Collections.reverse( lista );
                if ( lista != null && lista.size() > 0 ){

                    requisicao = lista.get( 0 );

                    if ( requisicao != null ){
                        if ( !requisicao.getStatus().equals( Requisicao.STATUS_ENCERRADA ) ) {
                            passageiro = requisicao.getPassageiro();
                            localPassageiro = new LatLng(
                                    Double.parseDouble(passageiro.getLatidude()),
                                    Double.parseDouble(passageiro.getLongitude())
                            );

                            statusRequisicao = requisicao.getStatus();
                            destino = requisicao.getDestino();
                            if (requisicao.getMotorista() != null) {
                                motorista = requisicao.getMotorista();
                                localMotorista = new LatLng(
                                        Double.parseDouble(motorista.getLatidude()),
                                        Double.parseDouble(motorista.getLongitude())
                                );
                            }
                            alteraInterfaceStatusRequisicao(statusRequisicao);
                        }
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void alteraInterfaceStatusRequisicao( String status ){

        if ( status != null && !status.isEmpty() ){
            cancelarUber = false;
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
                case Requisicao.STATUS_FINALIZADA :
                    requisicaoFinalizada();
                    break;

                case Requisicao.STATUS_CANCELADA:
                    requisicaoCancelada();
                    break;
            }
        }else {
            //Adiciona marcador passageiro
            adicionaMarcadorPassageiro( localPassageiro, "Seu local" );
            centralizarMarcador( localPassageiro );
        }
    }

    private void requisicaoCancelada(){

        binding.linearLayoutDestino.setVisibility( View.VISIBLE );
        binding.buttonChamarUber.setText("Chamar Uber");
        cancelarUber = false;

    }

    private void requisicaoAguardando(){
        binding.linearLayoutDestino.setVisibility( View.GONE );
        binding.buttonChamarUber.setText( "Cancelar Uber" );
        cancelarUber = true;

        //Adiciona marcador de passageiro
        adicionaMarcadorPassageiro( localPassageiro, passageiro.getNome() );
        centralizarMarcador( localPassageiro );
    }

    private void requisicaoACaminho(){
        binding.linearLayoutDestino.setVisibility( View.GONE );
        binding.buttonChamarUber.setText("Motorista a caminho");
        binding.buttonChamarUber.setEnabled( false );

        //Adiciona marcador passageiro
        adicionaMarcadorPassageiro( localPassageiro, passageiro.getNome() );

        //Adiciona marcador motorista
        adicionaMarcadorMotorista( localMotorista, motorista.getNome() );

        //Centralizar dois marcadores
        centralizarDoisMarcadores( marcadorMotorista, marcadorPassageiro );

    }

    private void requisicaoViagem(){
        binding.linearLayoutDestino.setVisibility( View.GONE );
        binding.buttonChamarUber.setText("A caminho do destino");
        binding.buttonChamarUber.setEnabled( false );

        //Adiciona marcador motorista
        adicionaMarcadorMotorista( localMotorista, motorista.getNome() );

        //Adiciona marcador de destino
        LatLng localDestino = new LatLng(
                Double.parseDouble( destino.getLatitude() ),
                Double.parseDouble( destino.getLongitude())
        );
        adicionaMarcadorPassageiro( localDestino, "Destino" );

        //Centralizar dois marcadores
        centralizarDoisMarcadores( marcadorMotorista, marcadorDestino );
    }

    private void requisicaoFinalizada() {
        binding.linearLayoutDestino.setVisibility( View.GONE );
        binding.buttonChamarUber.setEnabled( false );

        //Adiciona marcador de destino
        LatLng localDestino = new LatLng(
                Double.parseDouble( destino.getLatitude() ),
                Double.parseDouble( destino.getLongitude() )
        );
        adicionaMarcadorDestino( localDestino, "Destino" );
        centralizarMarcador( localDestino );

        //Calcular distância
        float distancia = Local.calcularDistancia( localPassageiro, localDestino );
        float valor = distancia * 8;
        DecimalFormat decimal = new DecimalFormat( "0.00" );
        String resultado = decimal.format( valor );

        binding.buttonChamarUber.setText( "Corrida finalizada - R$ " + resultado );

        AlertDialog.Builder builder =  new AlertDialog.Builder( this )
                .setTitle( "Total da viagem" )
                .setMessage( "Sua viagem ficou: R$ " + resultado )
                .setCancelable( false )
                .setNegativeButton("Encerrar viagem", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        requisicao.setStatus( Requisicao.STATUS_ENCERRADA );
                        requisicao.atualizarStatus();

                        finish();;
                        startActivity( new Intent( getIntent() ));

                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();

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

    private void centralizarMarcador( LatLng local ){
        mMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom( local, 20 ) );
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

    public void chamarUber( View view ){

        //false -> uber não pode ser cancelado ainda
        //true -> uber pode ser cancelado
        if ( cancelarUber){//Uber pode ser cancelado

            //Cancelar a requisição
            requisicao.setStatus( Requisicao.STATUS_CANCELADA );
            requisicao.atualizarStatus();

        }else {
            String enderecoDEstino = binding.editDestino.getText().toString();

            if ( !enderecoDEstino.equals("")  || enderecoDEstino != null ){

                Address addressDestino = repuperarEndereco( enderecoDEstino );

                if ( addressDestino != null ){

                    Destino destino = new Destino();
                    destino.setCidade( addressDestino.getAdminArea() );
                    destino.setCep( addressDestino.getPostalCode() );
                    destino.setBairro( addressDestino.getSubLocality() );
                    destino.setRua( addressDestino.getThoroughfare() );
                    destino.setNumero( addressDestino.getFeatureName() );
                    destino.setLatitude( String.valueOf( addressDestino.getLatitude() ) );
                    destino.setLongitude( String.valueOf( addressDestino.getLongitude() ) );

                    StringBuilder mensagem = new StringBuilder();
                    mensagem.append( "Cidade: " + destino.getCidade() );
                    mensagem.append( "\nRua: " + destino.getRua() );
                    mensagem.append( "\nBairro: " + destino.getBairro() );
                    mensagem.append( "\nNúmero: " + destino.getNumero() );
                    mensagem.append( "\n Cep: " + destino.getCep() );

                    AlertDialog.Builder builder = new AlertDialog.Builder( this )
                            .setTitle( "Confirme seu endereço!" )
                            .setMessage( mensagem )
                            .setPositiveButton("Confirmar", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                    //Salvar requisição
                                    salvarRequisicao(  destino );

                                }
                            }).setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                }
                            });

                    AlertDialog dialog = builder.create();
                    dialog.show();
                }

            }else {
                Toast.makeText(this, "Informe o endereço de destino!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void salvarRequisicao(Destino destino) {

        Requisicao requisicao = new Requisicao();
        requisicao.setDestino( destino );

        Usuario usuarioPassageiro = UsuarioFirebase.getDadosUsuarioLogado();
        usuarioPassageiro.setLatidude( String.valueOf( localPassageiro.latitude ) );
        usuarioPassageiro.setLongitude( String.valueOf( localPassageiro.longitude ) );

        requisicao.setPassageiro( usuarioPassageiro );
        requisicao.setStatus( Requisicao.STATUS_AGUARDANDO );
        requisicao.salvar();



    }

    private Address repuperarEndereco( String endereco ) {
        Geocoder geocoder = new Geocoder( this, Locale.getDefault() );
        try {
            List<Address> listaEnderecos = geocoder.getFromLocationName( endereco, 1 );

            if ( listaEnderecos != null && listaEnderecos.size() > 0 ){
                Address address = listaEnderecos.get( 0 );

               return address;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void recuperarLocalizacaoUsuario() {

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                //recuperar latitude e longitude
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                localPassageiro = new LatLng(latitude, longitude);

                //Atualizar Geofire
                UsuarioFirebase.atualizarDadosLocalizacao( latitude, longitude );

                //Alterar interface de acordo com o status
                alteraInterfaceStatusRequisicao( statusRequisicao );

                if ( statusRequisicao != null && !statusRequisicao.isEmpty() ) {
                    if (statusRequisicao.equals(Requisicao.STATUS_VIAGEM)
                            || statusRequisicao.equals(Requisicao.STATUS_FINALIZADA)) {
                        locationManager.removeUpdates(locationListener);
                    }else {
                        //Solicitar atualizações de localização
                        if (ActivityCompat.checkSelfPermission(PassageiroActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ) {
                            locationManager.requestLocationUpdates(
                                    LocationManager.NETWORK_PROVIDER,
                                    10000,
                                    10,
                                    locationListener
                            );
                        }
                    }
                }

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate( R.menu.menu_main, menu );
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch ( item.getItemId() ){
            case R.id.menuSair:
                autenticacao.signOut();
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}