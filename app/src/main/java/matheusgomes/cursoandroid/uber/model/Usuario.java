package matheusgomes.cursoandroid.uber.model;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;

import java.io.Serializable;

import matheusgomes.cursoandroid.uber.config.ConfiguracaoFirebase;

public class Usuario implements Serializable {

    private String id;
    private String nome;
    private String email;
    private String senha;
    private String tipo;

    private String latidude;
    private String longitude;

    public Usuario() {
    }

    public void salvar(){

        DatabaseReference firebaseRef = ConfiguracaoFirebase.getFirebaseDatabase();
        DatabaseReference usuarios = firebaseRef.child( "usuarios" ).child( getId() );

        usuarios.setValue(this);

    }

    public String getLatidude() {
        return latidude;
    }

    public void setLatidude(String latidude) {
        this.latidude = latidude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Exclude
    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
}
