package com.comerciosconecta.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "comercio")
public class Comercio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String nombre;
    private String nit;
    private String direccion;
    private String telefono;
    private String email;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // ── Personalización de la tienda online ─────────────────────────────
    @Column(name = "color_primario")
    private String colorPrimario = "#1F3B4D";

    @Column(name = "color_acento")
    private String colorAcento = "#00d4aa";

    private String tagline;

    @Column(name = "logo_url", columnDefinition = "TEXT")
    private String logoUrl;

    @Column(name = "hero_title")
    private String heroTitle;

    @Column(name = "hero_subtitle", columnDefinition = "TEXT")
    private String heroSubtitle;

    @Column(name = "hero_cta")
    private String heroCta;

    @Column(columnDefinition = "TEXT")
    private String categorias;

    @Column(name = "footer_texto")
    private String footerTexto;

    @Column(name = "footer_telefono")
    private String footerTelefono;

    @Column(name = "font_family")
    private String fontFamily;

    @Column(name = "button_radius")
    private String buttonRadius = "50px";

    @Column(name = "card_radius")
    private String cardRadius = "12px";

    @Column(name = "custom_css", columnDefinition = "TEXT")
    private String customCss;

    @Column(name = "facebook")
    private String facebook;

    @Column(name = "instagram")
    private String instagram;

    @Column(name = "twitter")
    private String twitter;

    @Column(name = "tiktok")
    private String tiktok;

    @Column(name = "whatsapp")
    private String whatsapp;

    public Comercio() {}

    public Comercio(String nombre, String nit, String direccion, String telefono, String email) {
        this.nombre = nombre;
        this.nit = nit;
        this.direccion = direccion;
        this.telefono = telefono;
        this.email = email;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getNit() { return nit; }
    public void setNit(String nit) { this.nit = nit; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getColorPrimario() { return colorPrimario; }
    public void setColorPrimario(String colorPrimario) { this.colorPrimario = colorPrimario; }

    public String getColorAcento() { return colorAcento; }
    public void setColorAcento(String colorAcento) { this.colorAcento = colorAcento; }

    public String getTagline() { return tagline; }
    public void setTagline(String tagline) { this.tagline = tagline; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public String getHeroTitle() { return heroTitle; }
    public void setHeroTitle(String heroTitle) { this.heroTitle = heroTitle; }

    public String getHeroSubtitle() { return heroSubtitle; }
    public void setHeroSubtitle(String heroSubtitle) { this.heroSubtitle = heroSubtitle; }

    public String getHeroCta() { return heroCta; }
    public void setHeroCta(String heroCta) { this.heroCta = heroCta; }

    public String getCategorias() { return categorias; }
    public void setCategorias(String categorias) { this.categorias = categorias; }

    public String getFooterTexto() { return footerTexto; }
    public void setFooterTexto(String footerTexto) { this.footerTexto = footerTexto; }

    public String getFooterTelefono() { return footerTelefono; }
    public void setFooterTelefono(String footerTelefono) { this.footerTelefono = footerTelefono; }

    public String getFontFamily() { return fontFamily; }
    public void setFontFamily(String fontFamily) { this.fontFamily = fontFamily; }

    public String getButtonRadius() { return buttonRadius; }
    public void setButtonRadius(String buttonRadius) { this.buttonRadius = buttonRadius; }

    public String getCardRadius() { return cardRadius; }
    public void setCardRadius(String cardRadius) { this.cardRadius = cardRadius; }

    public String getCustomCss() { return customCss; }
    public void setCustomCss(String customCss) { this.customCss = customCss; }

    public String getFacebook()  { return facebook; }
    public void setFacebook(String facebook)   { this.facebook = facebook; }

    public String getInstagram() { return instagram; }
    public void setInstagram(String instagram) { this.instagram = instagram; }

    public String getTwitter()   { return twitter; }
    public void setTwitter(String twitter)     { this.twitter = twitter; }

    public String getTiktok()    { return tiktok; }
    public void setTiktok(String tiktok)       { this.tiktok = tiktok; }

    public String getWhatsapp()  { return whatsapp; }
    public void setWhatsapp(String whatsapp)   { this.whatsapp = whatsapp; }
}
