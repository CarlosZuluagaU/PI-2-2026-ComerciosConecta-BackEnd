package com.comerciosconecta.backend.service;



import com.comerciosconecta.backend.entity.Comercio;
import com.comerciosconecta.backend.repository.ComercioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ComercioService {

    @Autowired
    private ComercioRepository comercioRepository;

    public Comercio registrarComercio(Comercio comercio) {
        // Validar duplicados por NIT o correo
        if (comercioRepository.findByNit(comercio.getNit()).isPresent()) {
            throw new RuntimeException("Ya existe un comercio con ese NIT");
        }

        if (comercioRepository.findByEmail(comercio.getEmail()).isPresent()) {
            throw new RuntimeException("Ya existe un comercio con ese correo electrónico");
        }

        return comercioRepository.save(comercio);
    }

    public List<Comercio> listarComercios() {
        return comercioRepository.findAll();
    }

    public Comercio obtenerApariencia(Long id) {
        return comercioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comercio no encontrado"));
    }

    public Comercio actualizarApariencia(Long id, Comercio datos) {
        Comercio comercio = comercioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comercio no encontrado"));

        if (datos.getNombre()        != null) comercio.setNombre(datos.getNombre());
        if (datos.getTagline()       != null) comercio.setTagline(datos.getTagline());
        if (datos.getLogoUrl()       != null) comercio.setLogoUrl(datos.getLogoUrl());
        if (datos.getColorPrimario() != null) comercio.setColorPrimario(datos.getColorPrimario());
        if (datos.getColorAcento()   != null) comercio.setColorAcento(datos.getColorAcento());
        if (datos.getHeroTitle()     != null) comercio.setHeroTitle(datos.getHeroTitle());
        if (datos.getHeroSubtitle()  != null) comercio.setHeroSubtitle(datos.getHeroSubtitle());
        if (datos.getHeroCta()       != null) comercio.setHeroCta(datos.getHeroCta());
        if (datos.getCategorias()    != null) comercio.setCategorias(datos.getCategorias());
        if (datos.getFooterTexto()   != null) comercio.setFooterTexto(datos.getFooterTexto());
        if (datos.getFooterTelefono()!= null) comercio.setFooterTelefono(datos.getFooterTelefono());
        if (datos.getFontFamily()    != null) comercio.setFontFamily(datos.getFontFamily());
        if (datos.getButtonRadius()  != null) comercio.setButtonRadius(datos.getButtonRadius());
        if (datos.getCardRadius()    != null) comercio.setCardRadius(datos.getCardRadius());
        if (datos.getCustomCss()     != null) comercio.setCustomCss(datos.getCustomCss());
        if (datos.getFacebook()      != null) comercio.setFacebook(datos.getFacebook());
        if (datos.getInstagram()     != null) comercio.setInstagram(datos.getInstagram());
        if (datos.getTwitter()       != null) comercio.setTwitter(datos.getTwitter());
        if (datos.getTiktok()        != null) comercio.setTiktok(datos.getTiktok());
        if (datos.getWhatsapp()               != null) comercio.setWhatsapp(datos.getWhatsapp());
        if (datos.getColorTexto()             != null) comercio.setColorTexto(datos.getColorTexto());
        if (datos.getColorTextoSecundario()   != null) comercio.setColorTextoSecundario(datos.getColorTextoSecundario());
        if (datos.getColorTextoBoton()        != null) comercio.setColorTextoBoton(datos.getColorTextoBoton());
        if (datos.getColorBoton()             != null) comercio.setColorBoton(datos.getColorBoton());
        if (datos.getColorBotonCta()          != null) comercio.setColorBotonCta(datos.getColorBotonCta());
        if (datos.getColorCarritoBoton()      != null) comercio.setColorCarritoBoton(datos.getColorCarritoBoton());
        if (datos.getColorBanner()            != null) comercio.setColorBanner(datos.getColorBanner());
        if (datos.getColorBannerSecundario()  != null) comercio.setColorBannerSecundario(datos.getColorBannerSecundario());
        if (datos.getColorFooterTexto()       != null) comercio.setColorFooterTexto(datos.getColorFooterTexto());
        if (datos.getColorIconosSociales()    != null) comercio.setColorIconosSociales(datos.getColorIconosSociales());
        if (datos.getColorNombre()            != null) comercio.setColorNombre(datos.getColorNombre());
        if (datos.getColorTagline()           != null) comercio.setColorTagline(datos.getColorTagline());
        if (datos.getLayout()                 != null) comercio.setLayout(datos.getLayout());
        if (datos.getHoverBtn()               != null) comercio.setHoverBtn(datos.getHoverBtn());

        return comercioRepository.save(comercio);
    }
}

