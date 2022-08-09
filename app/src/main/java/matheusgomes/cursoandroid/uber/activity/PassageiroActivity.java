package matheusgomes.cursoandroid.uber.activity;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
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
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import matheusgomes.cursoandroid.uber.R;
import matheusgomes.cursoandroid.uber.config.ConfiguracaoFirebase;
import matheusgomes.cursoandroid.uber.databinding.ActivityPassageiroBinding;
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

    private AppBarConfiguration appBarConfiguration;
    private ActivityPassageiroBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityPassageiroBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setTitle("Iniciar uma viagem");

        setSupportActionBar(binding.toolbar);

        //Configurações iniciais
        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

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
                        .setPositiveButton("COnfirmar", new DialogInterface.OnClickListener() {
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

                mMap.clear();
                mMap.addMarker(
                        new MarkerOptions()
                                .position( localPassageiro )
                                .title("Meu Local")
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.usuario))
                );
                mMap.moveCamera(
                        CameraUpdateFactory.newLatLngZoom( localPassageiro, 20 )
                );

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