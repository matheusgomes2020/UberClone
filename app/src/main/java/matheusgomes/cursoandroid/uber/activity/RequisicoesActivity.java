package matheusgomes.cursoandroid.uber.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import matheusgomes.cursoandroid.uber.R;
import matheusgomes.cursoandroid.uber.adapter.RequisicoesAdapter;
import matheusgomes.cursoandroid.uber.config.ConfiguracaoFirebase;
import matheusgomes.cursoandroid.uber.databinding.ActivityRequisicoesBinding;
import matheusgomes.cursoandroid.uber.helper.RecyclerItemClickListener;
import matheusgomes.cursoandroid.uber.helper.UsuarioFirebase;
import matheusgomes.cursoandroid.uber.model.Requisicao;
import matheusgomes.cursoandroid.uber.model.Usuario;

public class RequisicoesActivity extends AppCompatActivity {

    private ActivityRequisicoesBinding binding;

    private FirebaseAuth autenticacao;

    private DatabaseReference firbaseRef;

    private List<Requisicao> listaRequisicoes = new ArrayList<>();

    private Usuario motorista;

    private RequisicoesAdapter adapter;

    private LocationManager locationManager;

    private LocationListener locationListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityRequisicoesBinding.inflate( getLayoutInflater() );
        setContentView( binding.getRoot() );

        getSupportActionBar().setTitle( "Requisicoes" );

        //Configurações iniciais
        motorista = UsuarioFirebase.getDadosUsuarioLogado();
        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
        firbaseRef = ConfiguracaoFirebase.getFirebaseDatabase();

        //Recuperar localizacao do usuário
        recuperarLocalizacaoUsuario();

        //Configurar RecyclerVIew
        adapter = new RequisicoesAdapter( listaRequisicoes, getApplicationContext(), motorista );
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager( getApplicationContext() );
        binding.recyclerRequisicoes.setLayoutManager( layoutManager );
        binding.recyclerRequisicoes.setHasFixedSize( true );
        binding.recyclerRequisicoes.setAdapter( adapter );

        recuperarRequisicoes();

        //Adiciona evento de clique no recycler
        binding.recyclerRequisicoes.addOnItemTouchListener(
                new RecyclerItemClickListener(
                        getApplicationContext(),
                        binding.recyclerRequisicoes,
                        new RecyclerItemClickListener.OnItemClickListener() {
                            @Override
                            public void onItemClick(View view, int position) {

                                Requisicao requisicao = listaRequisicoes.get( position );
                                Intent i = new Intent( RequisicoesActivity.this, CorridaActivity.class );
                                i.putExtra( "idRequisicao", requisicao.getId() );
                                i.putExtra( "motorista", motorista );
                                startActivity( i );

                            }

                            @Override
                            public void onLongItemClick(View view, int position) {

                            }

                            @Override
                            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                            }
                        }
                )
        );

    }

    private void recuperarLocalizacaoUsuario() {

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                //recuperar latitude e longitude
                String latitude = String.valueOf( location.getLatitude() );
                String longitude = String.valueOf( location.getLongitude() );

                if ( !latitude.isEmpty() && !longitude.isEmpty() ){
                    motorista.setLatidude( latitude );
                    motorista.setLongitude( longitude );
                    locationManager.removeUpdates( locationListener );
                    adapter.notifyDataSetChanged();
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
                    0,
                    0,
                    locationListener
            );
        }


    }

    private void recuperarRequisicoes() {

        DatabaseReference requisicoes = firbaseRef.child( "requisicoes" );

        Query requisicaoPesquisa = requisicoes.orderByChild( "status" )
                .equalTo( Requisicao.STATUS_AGUARDANDO );

        requisicaoPesquisa.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if ( snapshot.getChildrenCount() > 0 ){
                    binding.textResultado.setVisibility( View.GONE );
                    binding.recyclerRequisicoes.setVisibility( View.VISIBLE );
                }else {
                    binding.textResultado.setVisibility( View.VISIBLE );
                    binding.recyclerRequisicoes.setVisibility( View.GONE);
                }

                for ( DataSnapshot ds : snapshot.getChildren() ){

                    Requisicao requisicao = ds.getValue( Requisicao.class );

                    listaRequisicoes.add( requisicao );

                }

                adapter.notifyDataSetChanged();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

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