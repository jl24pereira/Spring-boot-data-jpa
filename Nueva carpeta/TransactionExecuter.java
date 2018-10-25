/*
 * Created on Apr 24, 2004
 *
 */
package com.fintec.potala.bac.dq.trans;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import com.ba.comunes.Utils;
import com.ba.desarrollo.Entorno;
import com.ba.desarrollo.contenedores.Peticion;
import com.ba.desarrollo.contenedores.Respuesta;
import com.ba.desarrollo.interconexion.Conector;
import com.ba.pegaso.abstraccion.negocio.EstadoSolicitudNegocio;
import com.ba.pegaso.dependenciasGlobales.PegasoResolusor;
import com.ba.plataforma.servicio.comun.dto.dinamico.RequestDTODinamico;
import com.ba.plataforma.servicio.comun.dto.dinamico.ResponseDTODinamico;
import com.ba.servicios.integracion.Servicios;
import com.ba.servicios.integracion.exceptions.AtributoRequestException;
import com.bancoagricola.frmwrk.common.Helper;
import com.fintec.comunes.Constantes;
import com.fintec.definiciones.Moneda;
import com.fintec.hibernate.HibernateMap;
import com.fintec.potala.bac.dq.trans.doubles.AS400Doubles;
import com.fintec.potala.bac.dq.trans.doubles.PagoAduana;
import com.fintec.potala.bac.dq.trans.doubles.PagoAfp;
import com.fintec.potala.bac.dq.trans.doubles.PagoCENTREX;
import com.fintec.potala.bac.dq.trans.doubles.PagoISSS;
import com.fintec.potala.bac.dq.trans.doubles.PagoPrestamo;
import com.fintec.potala.bac.dq.trans.doubles.PagoReservacionTACA;
import com.fintec.potala.bac.dq.trans.doubles.PagoServicio;
import com.fintec.potala.bac.dq.trans.doubles.PagoServicioSinFactura;
import com.fintec.potala.bac.dq.trans.doubles.PagoTarjeta;
import com.fintec.potala.bac.dq.trans.doubles.TransferenciaPropia;
import com.fintec.potala.bac.dq.trans.doubles.TransferenciaTercero;
import com.fintec.potala.bac.dq.trans.multiples.AplicadorBoletaPago;
import com.fintec.potala.bac.dq.trans.multiples.MultipleAbonoNoPropioACH;
import com.fintec.potala.bac.dq.trans.multiples.Multiples;
import com.fintec.potala.bac.dq.trans.multiples.MultiplesAbonos;
import com.fintec.potala.bac.dq.trans.multiples.MultiplesAbonosPlanilla;
import com.fintec.potala.bac.dq.trans.multiples.MultiplesAbonosPlanillaEduco;
import com.fintec.potala.bac.dq.trans.multiples.MultiplesAbonosPlanillaIsssEduco;
import com.fintec.potala.bac.dq.trans.multiples.MultiplesAnticipoDocumentosAED;
import com.fintec.potala.bac.dq.trans.multiples.MultiplesCargosPlanilla;
import com.fintec.potala.bac.dq.trans.multiples.MultiplesCuentasPredefinidas;
import com.fintec.potala.bac.dq.trans.multiples.MultiplesConfirmacionChequesPlanilla;
import com.fintec.potala.bac.dq.trans.multiples.MultiplesPagosPlanillaServicios;
import com.fintec.potala.bac.dq.trans.multiples.MultiplesPagosProveedoresAED;
import com.fintec.potala.bac.dq.trans.multiples.MultiplesPrePublicacionDocumentosPR;
import com.fintec.potala.bac.dq.trans.multiples.MultiplesPublicacionDocumentosAE;
import com.fintec.potala.bac.dq.trans.multiples.PagoHacienda;
import com.fintec.potala.bac.dq.trans.simples.AS400Simple;
import com.fintec.potala.bac.dq.trans.simples.BloqueoTarjeta;
import com.fintec.potala.bac.dq.trans.simples.ConfirmacionCheque;
import com.fintec.potala.bac.dq.trans.simples.CotizacionMonedaEnLinea;
import com.fintec.potala.bac.dq.trans.simples.DatosUsuario;
import com.fintec.potala.bac.dq.trans.simples.ExtravioLibreta;
import com.fintec.potala.bac.dq.trans.simples.LiberacionFondos;
import com.fintec.potala.bac.dq.trans.simples.ConfirmacionChequera;
import com.fintec.potala.bac.dq.trans.simples.PagoAES;
import com.fintec.potala.bac.dq.trans.simples.RecargaCelular;
import com.fintec.potala.bac.dq.trans.simples.Reserva;
import com.fintec.potala.bac.dq.trans.simples.ReservacionCheque;
import com.fintec.potala.bac.dq.trans.simples.SolicitudChequera;
import com.fintec.potala.bac.dq.trans.simples.SolicitudConfirmacion;
import com.fintec.potala.bac.dq.trans.simples.SuspensionChequera;
import com.fintec.potala.bac.dq.trans.simples.TransferenciaInternacional;
import com.fintec.potala.corporativa.clazzes.EstadisticaTransaccion;
import com.fintec.potala.corporativa.clazzes.EstadoTransaccion;
import com.fintec.potala.corporativa.clazzes.LineaTransaccion;
import com.fintec.potala.corporativa.clazzes.MQMensajeDevolucion;
import com.fintec.potala.corporativa.clazzes.MQResultado;
import com.fintec.potala.corporativa.clazzes.TipoTransaccion;
import com.fintec.potala.corporativa.clazzes.Transaccion;
import com.fintec.potala.corporativa.clazzes.TransaccionFlags;
import com.fintec.potala.corporativa.clazzes.Usuario;
import com.fintec.potala.corporativa.services.TransaccionUtils;
import com.fintec.potala.corporativa.services.TransferenciaUtils;
import com.fintec.potala.services.mail.Mailer;
import com.fintec.potala.struts.clases.comprobantes.Comprobante;
import com.fintec.potala.struts.ec.pdf.FormularioPDF;
import com.fintec.potala.struts.forms.solicitudes.SolicitudTransferenciaInternacionalForm;
import com.fintec.potala.struts.services.AuditoriaUsuarioUtils;
import com.fintec.potala.web.bac.jdbc.db.DatosConvenioDesembLC;
import com.fintec.potala.web.bac.jdbc.db.DatosDesembolsoCrediPOS;
import com.fintec.potala.web.bac.jdbc.db.SolicitudAfiliacionTrxAutomaticaDB;
import com.fintec.potala.web.bac.jdbc.db.SolicitudCartaCreditoDB;
import com.fintec.potala.web.bac.jdbc.db.SolicitudIncorporacionProveedorDB;
import com.fintec.potala.web.bac.jdbc.db.SolicitudTransferenciaACHDB;
import com.fintec.potala.web.bac.jdbc.db.TransferenciaInternacionalDB;
import com.fintec.potala.web.bac.jdbc.sql.OperacionesACHSQL;
import com.fintec.potala.web.bac.jdbc.sql.RutaCorreo;
import com.fintec.potala.web.bac.jdbc.sql.solicitudes.SolicitudCartaCreditoSQL;
import com.fintec.potala.web.bac.jdbc.sql.solicitudes.SolicitudRutaClienteSQL;
import com.fintec.potala.web.clases.ObservacionPlanillas;
import com.fintec.potala.web.clases.solicitudes.SolicitudAfiliacionTrxAutomaticas;
import com.fintec.potala.web.clases.solicitudes.SolicitudCartaCredito;
import com.fintec.potala.web.clases.solicitudes.SolicitudChequeCaja;
import com.fintec.potala.web.clases.solicitudes.SolicitudCuentaAhorro;
import com.fintec.potala.web.clases.solicitudes.SolicitudCuentaCorriente;
import com.fintec.potala.web.clases.solicitudes.SolicitudDepositoPlazo;
import com.fintec.potala.web.clases.solicitudes.SolicitudDesembolsos;
import com.fintec.potala.web.clases.solicitudes.SolicitudEnmiendaCartaCredito;
import com.fintec.potala.web.clases.solicitudes.SolicitudGenerico;
import com.fintec.potala.web.clases.solicitudes.SolicitudPrestamo;
import com.fintec.potala.web.clases.solicitudes.SolicitudTarjetaCredito;
import com.fintec.potala.web.clases.solicitudes.SolicitudTransferenciaInternacional;
import com.fintec.potala.web.clases.solicitudes.afiliaciontrxautom.SolicitudAfiliacionTrxAutomaticaPagina1;
import com.fintec.potala.web.clases.solicitudes.cartacredito.SolicitudCartaCreditoPagina1;
import com.fintec.potala.web.clases.solicitudes.cartacredito.SolicitudCartaCreditoPagina2;
import com.fintec.potala.web.clases.solicitudes.cartacredito.SolicitudCartaCreditoPagina3;
import com.fintec.potala.web.clases.solicitudes.cartacredito.SolicitudCartaCreditoPagina4;
import com.fintec.potala.web.clases.solicitudes.cartacredito.SolicitudCartaCreditoPagina5;
import com.fintec.potala.web.clases.solicitudes.cartacredito.SolicitudCartaCreditoPagina6;
import com.fintec.potala.web.clases.solicitudes.transferenciainternacional.Pagina2DatosSolicitante;
import com.fintec.potala.web.clases.solicitudes.transferenciainternacional.Pagina3DatosTransaccion;
import com.fintec.seguridad.TipoMovimiento;
import com.fintec.sistema.Sistema;
import com.ofbox.ba.potala.transaction.imp.CargaQuedanTransaction;
import com.ofbox.ba.potala.transaction.imp.ChequesVencidosTransaction;
import com.fintec.potala.corporativa.clazzes.TransaccionHistorica;
import com.ba.planillasautomaticas.utils.Utilitarios;
import com.fintec.potala.bac.dq.trans.multiples.MultiplesIdentificacionDepositos;

import com.fintec.potala.bac.dq.trans.simples.EliminacionCodigosIdentif;
import com.fintec.potala.bac.dq.trans.multiples.MultiplesVerificaCuentasPlanilla;

import com.fintec.potala.web.bac.server.params.PropertyMaps;
/**
 * @author lmora
 *
 */
public class TransactionExecuter {
    private Usuario mUsuario = null;
    private Transaccion mTransaccion = null;
    private EstadisticaTransaccion mEstadistica = null;
    /**
     * constructor
     * @param transaccion
     */
    public TransactionExecuter(Transaccion transaccion, Usuario usuario) throws Exception {
        setTransaccion(transaccion);
        setEstadisticaFromTransaccion();
        getEstadistica().setTransaccion(transaccion.getId());
        getEstadistica().setFechaAplicacion(System.currentTimeMillis());
        getEstadistica().setFechaTerminoAplicacion(System.currentTimeMillis());
        getTransaccion().setMemoExtraAsBytes(TransaccionUtils.putEstadisticaToExtra(getTransaccion().getMemoExtraAsBytes(), getEstadistica()));
        mUsuario = usuario;
    }
    
    /**
     * @return
     */
    public EstadisticaTransaccion getEstadistica() {
        return mEstadistica;
    }
    
    /**
     * @param transaccion
     */
    public void setEstadistica(EstadisticaTransaccion transaccion) {
        mEstadistica = transaccion;
    }
    
    public void setEstadisticaFromTransaccion() throws Exception {
        mEstadistica = TransaccionUtils.getEstadisticaFromExtra(getTransaccion().getMemoExtraAsBytes());
    }
    
    
    /**
     * @return
     */
    private Transaccion getTransaccion() {
        return mTransaccion;
    }
    
    /**
     * @param transaccion
     */
    private void setTransaccion(Transaccion transaccion) {
        mTransaccion = transaccion;
    }
    
    /**
     * @return
     */
    public Usuario getUsuario() {
        return mUsuario;
    }
    
    /**
     * @param usuario
     */
    public void setUsuario(Usuario usuario) {
        mUsuario = usuario;
    }
    
    private void doStatistic(AS400Simple operacion) throws Exception {
        if (operacion.getResultado() < 2) {
            LineaTransaccion line = (LineaTransaccion) getTransaccion().getLineaTransaccionList().get(0);
            getEstadistica().setCantidadAplicadoDebito(1);
            getEstadistica().setTotalAplicadoDebito(line.getMonto());
        }
    }
    
    private void doStatistic(AS400Doubles operacion) throws Exception {
        if (operacion.getResultado() < 2) {
            LineaTransaccion line = (LineaTransaccion) getTransaccion().getLineaTransaccionList().get(0);
            getEstadistica().setCantidadAplicadoDebito(1);
            getEstadistica().setCantidadAplicadoCredito(1);
            getEstadistica().setTotalAplicadoDebito(line.getMonto());
            getEstadistica().setTotalAplicadoCredito(line.getMonto());
            //-- Se procede a notificar al monitor de Transferencia a Terceros
            try {
                if (operacion.getTransaccion().getTipoTransaccion().equals(TipoTransaccion.TRANSFERENCIA_CUENTAS_TERCEROS)) {
                	TransferenciaUtils util = new TransferenciaUtils();
                	Transaccion trx = operacion.getTransaccion();
                	util.notificarMonitorTransferencias(trx.getUsuario(), trx.getId(), trx.getTipoTransaccion().getId(), trx.getMontoDebitos(), 
                			trx.getFechaAplicacion(), trx.getLineaTransaccion(0).getNumeroComprobante(), 
							trx.getLineaTransaccion(0).getCuentaAsString(), trx.getLineaTransaccion(1).getCuentaAsString());
                }
            } catch (Exception e) {
            	System.err.println("TransactionExecuter.doStatistic() Falla al guardar log Transferencias Terceros: "+e.getMessage());
            }
        }
    }
    private void doStatistic(Multiples operacion) throws Exception {
        if (operacion.getResultado() < 2) {
            getEstadistica().setCantidadAplicadoDebito(operacion.getCantidadRetiro());
            getEstadistica().setCantidadAplicadoCredito(operacion.getCantidadAbono());
            getEstadistica().setTotalAplicadoDebito(operacion.getMontoRetiro());
            getEstadistica().setTotalAplicadoCredito(operacion.getMontoAbono());
            getEstadistica().setComisionCobrada(operacion.getComision());
            getEstadistica().setImpuesto(operacion.getIVA());
        }
	}
	
    /**
     * @param form
     */
    private void doSolicitudCartaCredito(SolicitudCartaCredito solicitud) throws Exception{
        getEstadistica().setCantidadAplicadoDebito(1);
        
        //----Todas las solicitudes tiene una sola linea de transaccion
        LineaTransaccion linea = (LineaTransaccion) getTransaccion().getLineaTransaccionList().get(0);
        solicitud.fromBytes(linea.getMemoAsBytes());
        
        //----Tomamos todas las paginas
        SolicitudCartaCreditoPagina1 pagina1 = (SolicitudCartaCreditoPagina1) solicitud.getPagina1();
        SolicitudCartaCreditoPagina2 pagina2 = (SolicitudCartaCreditoPagina2) solicitud.getPagina2();
        SolicitudCartaCreditoPagina3 pagina3 = (SolicitudCartaCreditoPagina3) solicitud.getPagina3();
        SolicitudCartaCreditoPagina4 pagina4 = (SolicitudCartaCreditoPagina4) solicitud.getPagina4();
        SolicitudCartaCreditoPagina5 pagina5 = (SolicitudCartaCreditoPagina5) solicitud.getPagina5();
        SolicitudCartaCreditoPagina6 pagina6 = (SolicitudCartaCreditoPagina6) solicitud.getPagina6();
        
        SolicitudCartaCreditoDB solicitudCartaCreditoDB = new SolicitudCartaCreditoDB();
        solicitudCartaCreditoDB.setCliente("" + solicitud.getCliente().getId());
        
        //----Pagina 1
        solicitudCartaCreditoDB.setTerminosCondiciones(pagina1.getTerminosCondiciones());
        solicitudCartaCreditoDB.setCuenta(pagina1.getCuentaCargar().getNumero());
        solicitudCartaCreditoDB.setDuracion(pagina1.getDuracion());
        solicitudCartaCreditoDB.setPaisNegociacion(pagina1.getPaisNegociacion());
        solicitudCartaCreditoDB.setTextoValorCartaCredito(pagina1.getTextoValorCartaCredito());
        solicitudCartaCreditoDB.setRadioValorCartaCredito(pagina1.getRadioValorCartaCredito());
        solicitudCartaCreditoDB.setFormaPago(pagina1.getFormaPago());
        solicitudCartaCreditoDB.setNumeroDias(pagina1.getNumeroDias());
        
        //----Pagina2
        solicitudCartaCreditoDB.setNombreSol(pagina2.getNombre());
        solicitudCartaCreditoDB.setDireccionSol(pagina2.getDireccion());
        solicitudCartaCreditoDB.setContactoSol(pagina2.getContacto());
        solicitudCartaCreditoDB.setEmailSol(pagina2.getEmail());
        solicitudCartaCreditoDB.setTelefonoSol(pagina2.getTelefono());
        solicitudCartaCreditoDB.setFaxSol(pagina2.getFax());
        
        //----Pagina3
        solicitudCartaCreditoDB.setNombreBen(pagina3.getNombre());
        solicitudCartaCreditoDB.setCiudadBen(pagina3.getCiudad());
        solicitudCartaCreditoDB.setPaisBen(pagina3.getPais());
        solicitudCartaCreditoDB.setDireccionBen(pagina3.getDireccion());
        solicitudCartaCreditoDB.setContactoBen(pagina3.getContacto());
        solicitudCartaCreditoDB.setEmailBen(pagina3.getEmail());
        solicitudCartaCreditoDB.setTelefonoBen(pagina3.getTelefono());
        solicitudCartaCreditoDB.setFaxBen(pagina3.getFax());
        
        //----Pagina4
        solicitudCartaCreditoDB.setEmbarquesParciales(pagina4.getEmbarquesParciales());
        solicitudCartaCreditoDB.setTransbordos(pagina4.getTransbordos());
        solicitudCartaCreditoDB.setEmbarqueDesde(pagina4.getEmbarqueDesde());
        solicitudCartaCreditoDB.setEmbarqueHasta(pagina4.getEmbarqueHasta());
        solicitudCartaCreditoDB.setDescripcionMercaderia(pagina4.getDescripcionMercaderia());
        
        //----Fecha
        GregorianCalendar fechaDesde = new GregorianCalendar();
        fechaDesde.set(pagina4.getFechaEmbarqueAnno(), pagina4.getFechaEmbarqueMes() - 1, pagina4.getFechaEmbarqueDia());
        
        solicitudCartaCreditoDB.setFechaEmbarque(fechaDesde.getTime());
        solicitudCartaCreditoDB.setFlete(pagina4.getFlete());
        solicitudCartaCreditoDB.setTerminosEmbarque(pagina4.getTerminosEmbarque());
        solicitudCartaCreditoDB.setSeguroCubiertoPor(pagina4.getSeguroCubiertoPor());
        solicitudCartaCreditoDB.setNumeroPoliza(pagina4.getNumeroPoliza());
        
        //-----Pagina5
        solicitudCartaCreditoDB.setFacturaComercialCopias(pagina5.getFacturaComercialCopias());
        solicitudCartaCreditoDB.setFacturaComercialOriginales(pagina5.getFacturaComercialOriginales());
        solicitudCartaCreditoDB.setConocimientoEmbarqueCombo(pagina5.getConocimientoEmbarqueCombo());
        solicitudCartaCreditoDB.setConocimientoEmbarqueCopias(pagina5.getConocimientoEmbarqueCopias());
        solicitudCartaCreditoDB.setConocimientoEmbarqueOriginales(pagina5.getConocimientoEmbarqueOriginales());
        solicitudCartaCreditoDB.setPolizaCertificadoCopias(pagina5.getPolizaCertificadoCopias());
        solicitudCartaCreditoDB.setPolizaCertificadoOriginales(pagina5.getPolizaCertificadoOriginales());
        solicitudCartaCreditoDB.setListadoEmpaqueCopias(pagina5.getListadoEmpaqueOriginal());
        solicitudCartaCreditoDB.setListadoPesoCopias(pagina5.getListadoPesoCopias());
        solicitudCartaCreditoDB.setListadoPesoOriginal(pagina5.getListadoPesoOriginal());
        solicitudCartaCreditoDB.setCertificadoOrigenCopias(pagina5.getCertificadoOrigenCopias());
        solicitudCartaCreditoDB.setCertificadoOrigenOriginal(pagina5.getCertificadoOrigenOriginal());
        solicitudCartaCreditoDB.setOpcional1(pagina5.getOpcional1());
        solicitudCartaCreditoDB.setOpcional1Copias(pagina5.getOpcional1Copias());
        solicitudCartaCreditoDB.setOpcional1Original(pagina5.getOpcional1Original());
        solicitudCartaCreditoDB.setOpcional2(pagina5.getOpcional2());
        solicitudCartaCreditoDB.setOpcional2Copias(pagina5.getOpcional2Copias());
        solicitudCartaCreditoDB.setOpcional2Original(pagina5.getOpcional2Original());
        
        //----Pagina6
        solicitudCartaCreditoDB.setComisionesFuera(pagina6.getComisionesFuera());
        solicitudCartaCreditoDB.setComisionesLocales(pagina6.getComisionesLocales());
        solicitudCartaCreditoDB.setInteresesDescuento(pagina6.getInteresesDescuento());
        solicitudCartaCreditoDB.setInstruccionesEspeciales(pagina6.getInstruccionesEspeciales());
        
        SolicitudCartaCreditoSQL solicitudCartaCreditoSQL = new SolicitudCartaCreditoSQL(solicitudCartaCreditoDB);
        solicitudCartaCreditoSQL.save();
        
        //----Actualizamos el resultado de la linea
        setResultadoLineas();
    }
    
    /**
     *
     * @param solicitud
     * @throws Exception
     */
    private void enviarCorreo(SolicitudGenerico solicitud) throws Exception {
        getEstadistica().setCantidadAplicadoDebito(1);
        String tipoCliente = SolicitudRutaClienteSQL.getTipoCliente(getTransaccion().getCliente().getId());
        
        Mailer correo = new Mailer();
        
        //----Todas las solicitudes tiene una sola linea de transaccion
        LineaTransaccion linea = (LineaTransaccion) getTransaccion().getLineaTransaccionList().get(0);
        solicitud.fromBytes(linea.getMemoAsBytes());
        
        String mensaje[] = new String[] { solicitud.toString()};
        correo.sendMail(RutaCorreo.direccionEnviar(tipoCliente), solicitud.getFromSubjetCorreo(), solicitud.getFromCorreo(), mensaje);
        setResultadoLineas();
    }
    
    private void enviarCorreoTransferenciaInternacional(SolicitudGenerico solicitud, boolean esDolar) throws Exception {
    	String correoDivisionInter = Sistema.getProperty(Constantes.MAIL_USER_TRANSFERENCIA_INTER);
    	String[] to = null;
    	if(esDolar){
    		to = new String[] { correoDivisionInter };
    	}else{
    		//si es moneda extrajera enviara correo a Mesa Tasa de Cambio y Mesa de Distribucion y Fideicomiso
    		String otros = Sistema.getProperty(Constantes.MAIL_USER_TRANSFERENCIA_INTER_MONEDA);
        	otros = otros + "," + correoDivisionInter;
        	to = otros.split(",");
    	}
    	Mailer correo = new Mailer();
    	Transaccion transaccion = getTransaccion();

    	//----Todas las solicitudes tiene una sola linea de transaccion
    	LineaTransaccion linea = (LineaTransaccion) getTransaccion().getLineaTransaccionList().get(0);
    	solicitud.fromBytes(linea.getMemoAsBytes());
    	System.out.println("HEMOS ENTRADO A METODO PARA ENVIAR CORREO Y GENERAMOS PDF"); 
    	String codigoFormulario = "";
    	if(linea.getTramaEntradaAsString().trim().length() > 45) {    	
    		codigoFormulario = linea.getTramaEntradaAsString().substring(35, 45);
    	}

    	String mensaje[] = new String[] { solicitud.toString(codigoFormulario,getUsuario().getUsuario().getEmail())};

        FormularioPDF formularioPDF = new FormularioPDF();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        //FileOutputStream outputStream = new FileOutputStream("C://contrato/prueba.pdf");
        if(formularioPDF.drawPdf(transaccion, outputStream)) {
            byte[] bytes = outputStream.toByteArray();
                //envia el correo con pdf de formulario de egreso de divisas a la division internacional
                correo.sendMailWithPDF(to, solicitud.getFromSubjetCorreo(), solicitud.getFromCorreo(), solicitud.toString(codigoFormulario,getUsuario().getUsuario().getEmail()), null, bytes);
                //envia correo con pdf de formulario de egreso de divisas al usuario
                correo.sendMailWithPDF(new String[]{getUsuario().getUsuario().getEmail()}, solicitud.getFromSubjetCorreo(), solicitud.getFromCorreo(), solicitud.toString(codigoFormulario,getUsuario().getUsuario().getEmail()), null, bytes);
        } else {
            //envia correo a usuario
            correo.sendMail(getUsuario().getUsuario().getEmail(), solicitud.getFromSubjetCorreo(), solicitud.getFromCorreo(), mensaje);
            //envia correo a division internacional
            correo.sendMail(to, solicitud.getFromSubjetCorreo(), solicitud.getFromCorreo(), mensaje);
        }
    }
    
    private void enviarCorreoCotizacionMoneda() throws Exception {
    	String correoCotizacionMoneda = Sistema.getProperty(Constantes.MAIL_ADMINISTRADOR_MONEDA);	
    	String[] to = correoCotizacionMoneda.split(",");
    	Mailer correo = new Mailer();
    	String from = null;
    	String titulo = "Solicitud de cotización de moneda extranjera en línea";
    	Transaccion transaccion = getTransaccion();

    	//----Todas las cotizaciones de moneda tienen una sola linea de transaccion
    	LineaTransaccion linea = (LineaTransaccion) getTransaccion().getLineaTransaccionList().get(0);
    	System.out.println("HEMOS ENTRADO A METODO PARA ENVIAR CORREO POR COTIZACION DE MONEDA"); 
    	
        String tradeTicket = linea.getNumeroComprobante();
        String fechaVencimiento = linea.getTramaSalidaAsString();
        String[] tramaEntradaAsString = linea.getTramaEntradaAsString().split(",");
        String montoCotizado = tramaEntradaAsString[5];
        String tasaDeCambio = tramaEntradaAsString[3];
        String moneda = tramaEntradaAsString[0];
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss aaa");
        String fechaAplicacion = sdf.format(transaccion.getFechaAplicacion());

    	String mensaje[] = new String[] {"<hr>","Fecha y Hora de Creación: "+ fechaAplicacion +
    			"<hr>No. Cliente: " + transaccion.getCliente().getId() +
        		"<br>Cliente: "+getTransaccion().getUsuario().getCliente().getNombre()+"<br><hr> Moneda: " + moneda + 
        		"<br>Monto Cotizado: " + montoCotizado +
        		"<br>Tasa de cambio: " + tasaDeCambio +
        		"<br>Monto en dólares: " + linea.getMonto() +
        		"<hr>" +
        		"Trade Ticket: " + tradeTicket +
        		"<br>Fecha y hora de vencimiento Trade Ticket: " + fechaVencimiento};

    	correo.sendMail(to, titulo, from, mensaje);
    }
    
    /**
     * Actualiza todas las lineas colocandolas como aplicadas, esto es necesario
     * para las solicitudes donde algunas son por correo otras por jdbc.
     * @throws Exception
     */
    private void setResultadoLineas() throws Exception{
        //----tomas las lineas y por cada una las guardamos como aplicadas
        List lineas = getTransaccion().getLineaTransaccionList();
        LineaTransaccion linea;
        for (int i = 0; i < lineas.size(); i++){
            linea = (LineaTransaccion) lineas.get(i);
            linea.setResultado(MQResultado.APLICADO_MQ);
            linea.setMensajeDevolucion(MQMensajeDevolucion.APLICADA);
            HibernateMap.update(linea);
        }
    }
    
    /**
     * Actualiza todas las lineas colocandolas como RECHAZADAS, esto es necesario
     * para las solicitudes donde algunas son por correo otras por jdbc y otras
     * a traves de servicios.
     * @throws Exception
     */
    private void setResultadoLineasRechazadas(int argCodMsg) throws Exception{
        //----tomas las lineas y por cada una las guardamos como aplicadas
        List lineas = getTransaccion().getLineaTransaccionList();
        LineaTransaccion linea;
        for (int i = 0; i < lineas.size(); i++){
            linea = (LineaTransaccion) lineas.get(i);
            linea.setResultado(MQResultado.NO_PROCESADO);
            linea.setMensajeDevolucion(argCodMsg);
            HibernateMap.update(linea);
        }
    }
    
    /**
     *
     * @return
     * @throws Exception
     */
    public int doTransaction() throws Exception {
		System.out.println("Entrando a metodo doTransaction()..."); 
        int result = MQResultado.NO_PROCESADO;
        Transaccion transaccion = getTransaccion();
        TipoTransaccion tipo = transaccion.getTipoTransaccion();
		System.out.println("Actualizando Lineas de Transaccion..."); 

        TransaccionUtils.actualizarLineasTransaccion(transaccion);
		System.out.println("Se procede a validar si la transaccion aplica a validar cuadre de cargos y abonos...");
		System.out.println("getTransaccion().getLineaTransaccionList(): "+getTransaccion().getLineaTransaccionList().size());
        /*Validando que contenga lineas de transaccion y si es planilla, que cuadren cargos y abonos*/
        if(TransaccionUtils.validarCuadreCargosAbonos(getTransaccion().getTipoTransaccion())){
    		System.out.println("Es una transaccion apta para cuadrar cargues y abonos..."); 
        	if(getTransaccion().getLineaTransaccionList() == null || getTransaccion().getLineaTransaccionList().size()<=0){
        		System.out.println("La transaccion no tiene listas de transaccion, se procedera a Rechazar"); 
        		getTransaccion().setReferencia(getTransaccion().getReferencia()+". No existen lineas de detalle");
            	getTransaccion().setEstado(EstadoTransaccion.ESTADO_RECHAZADA);
       		 // Lo guarda como rechazado
               HibernateMap.update(getTransaccion());
               TransaccionUtils.pasarTransaccionAHistorica(getTransaccion());
               result=MQConstants.RESULT_CODE_RECHAZADA;
	            return result;
        	}else{
        		System.out.println("La transaccion si tiene listas de transacciones, se procedera a validar cargos y abonos ..."); 
        		double diferencia = TransaccionUtils.obtenerDiferenciaPlanillas(getTransaccion().getId());
	        	if(diferencia != 0){
	        		System.out.println("NO cuadran cargos y abonos, se procedera a Rechazar..."); 
	        		getTransaccion().setReferencia(getTransaccion().getReferencia()+". No cuadran los cargos con los abonos");
	        		getTransaccion().setEstado(EstadoTransaccion.ESTADO_RECHAZADA);
	        		 // Lo guarda como rechazado
	                HibernateMap.update(getTransaccion());
	                TransaccionUtils.pasarTransaccionAHistorica(getTransaccion());
	                result=MQConstants.RESULT_CODE_RECHAZADA;
	        		return result;                        
	        	}
        	}
        }else if(getTransaccion().getLineaTransaccionList() == null || getTransaccion().getLineaTransaccionList().isEmpty()){
    		System.out.println("No existen lineas de transaccion [Esta transacción no es tipo planilla] procedemos a rechazar...");       	
    		getTransaccion().setReferencia(getTransaccion().getReferencia()+". No existen lineas de detalle");
    		getTransaccion().setEstado(EstadoTransaccion.ESTADO_RECHAZADA);
    		 // Lo guarda como rechazado
            HibernateMap.update(getTransaccion());
            TransaccionUtils.pasarTransaccionAHistorica(getTransaccion());
            

        	return MQConstants.RESULT_CODE_RECHAZADA; 
        }
        
		System.out.println("Termino la validacion de cuadre de cargos y abonos y validacion de existencia de lineas de transaccion..."); 
		System.out.println("Siguiendo con proceso normal......"); 
        boolean bancaPersonas = false;
        try {
        	String codigoPersonas = Sistema.getProperty("potala-web-personal-aplicacion");
        	if (codigoPersonas != null && ! codigoPersonas.equals("")) {
        		bancaPersonas = true;
        	}
        } catch (Exception ex) {}
        
        if (TipoTransaccion.BLOQUEO_TARJETA.equals(tipo)) {
            BloqueoTarjeta bloqueo = new BloqueoTarjeta(getTransaccion());
            result = bloqueo.doIt();
            doStatistic(bloqueo);
        /*} else if (TipoTransaccion.CARGOS_A_TERCEROS.equals(tipo)){ //|| 
        		//TipoTransaccion.CARGOS_AUTOMATICOS.equals(tipo)) {
            CargosTercero pago = new CargosTercero(getTransaccion());
            result = pago.doIt();
            doStatistic(pago);*/
        } else if (TipoTransaccion.CARGOS_AUTOMATICOS.equals(tipo) || TipoTransaccion.CARGOS_A_TERCEROS.equals(tipo)) {
            MultiplesCargosPlanilla pago = new MultiplesCargosPlanilla(getTransaccion());
            result = pago.doIt();
            doStatistic(pago);
        } else if (TipoTransaccion.CONFIRMACION_CHEQUES.equals(tipo)) {
            ConfirmacionCheque confirma = new ConfirmacionCheque(getTransaccion());
            result = confirma.doIt();
            doStatistic(confirma);
        } else if (TipoTransaccion.PLANILLA_CONFIRMACION_CHEQUES.equals(tipo)){
        	MultiplesAbonos confirmaPlanilla = new MultiplesConfirmacionChequesPlanilla(getTransaccion());
        	//ConfirmacionChequePlanilla confirmaPlanilla = new ConfirmacionChequePlanilla(getTransaccion());
            result = confirmaPlanilla.doIt();
            doStatistic(confirmaPlanilla);
        } else if (TipoTransaccion.EXTRAVIO_LIBRETAS.equals(tipo)) {
            ExtravioLibreta extravio = new ExtravioLibreta(getTransaccion());
            result = extravio.doIt();
            doStatistic(extravio);
        } else if (TipoTransaccion.RESERVA_CHEQUE.equals(tipo)) {
            ReservacionCheque reserva = new ReservacionCheque(getTransaccion());
            result = reserva.doIt();
            doStatistic(reserva);
        } else if (TipoTransaccion.SOLICITUD_CONFIRMACION_CHEQUERAS.equals(tipo)) {
            SolicitudConfirmacion solicitud = new SolicitudConfirmacion(getTransaccion());
            result = solicitud.doIt();
            doStatistic(solicitud);
        } else if (TipoTransaccion.SUSPENSION_CHEQUES.equals(tipo)) {
            SuspensionChequera suspension = new SuspensionChequera(getTransaccion());
            result = suspension.doIt();
            doStatistic(suspension);
        } else if (TipoTransaccion.PAGO_PRESTAMOS.equals(tipo)) {
            PagoPrestamo pago = new PagoPrestamo(getTransaccion());
            result = pago.doIt();
            doStatistic(pago);
        }else if (TipoTransaccion.PAGO_ISSS.equals(tipo)) {
            PagoISSS pago = new PagoISSS(getTransaccion());
            result = pago.doIt();
            doStatistic(pago);
        } else if (TipoTransaccion.PAGO_SERVICIOS.equals(tipo) 
        		|| TipoTransaccion.PAGO_IMPUESTO_POR_NPE.equals(tipo)) {
        	Transaccion trans = getTransaccion();
        	//verifica que no sea pago por impuesto por npe y si es colectro AES        	
        	if(!TipoTransaccion.PAGO_IMPUESTO_POR_NPE.equals(tipo) && esColectorAes(trans)){
        		PagoAES pago = new PagoAES(trans);
        		result = pago.doIt();
        		doStatistic(pago);
        	}else{
        		PagoServicio pago = new PagoServicio(trans);
                result = pago.doIt();
                doStatistic(pago);       		
        	}
        }else if (TipoTransaccion.PAGO_SERVICIOS_SIN_NPE.equals(tipo)){//JMenendez 12092017
        	Transaccion trans = getTransaccion();
        	PagoServicioSinFactura pago = new PagoServicioSinFactura(trans);
            result = pago.doIt();
            doStatistic(pago);
        }else if (TipoTransaccion.PAGO_SERVICIOS_AFP.equals(tipo)) {
        	Transaccion trans = getTransaccion();
        
        		PagoServicio pago = new PagoServicio(trans);
                result = pago.doIt();
                doStatistic(pago);       		
        	
        }else if (TipoTransaccion.PAGO_MULTIPLE_SERVICIOS.equals(tipo)) {
            MultiplesAbonos pago = new MultiplesPagosPlanillaServicios(getTransaccion());
            result = pago.doIt();
            doStatistic(pago);            
        } else if (TipoTransaccion.PAGO_TARJETA_CREDITO.equals(tipo)) {
            PagoTarjeta pago = new PagoTarjeta(getTransaccion());
            result = pago.doIt();
            doStatistic(pago);
        } else if (TipoTransaccion.TRANSFERENCIA_CUENTAS_PROPIAS.equals(tipo)) {
            TransferenciaPropia transferencia = new TransferenciaPropia(getTransaccion());
            result = transferencia.doIt();
            doStatistic(transferencia);
        } else if (TipoTransaccion.TRANSFERENCIA_CUENTAS_TERCEROS.equals(tipo)) {
            TransferenciaTercero transferencia = new TransferenciaTercero(getTransaccion());
            result = transferencia.doIt();
            doStatistic(transferencia);
        } else if (TipoTransaccion.PAGO_POLIZA_IMPORTACION.equals(tipo)) {
            PagoAduana pago = new PagoAduana(getTransaccion());
            result = pago.doIt();
            doStatistic(pago);
        } else if (TipoTransaccion.ABONO_PENSIONADOS.equals(tipo)) {
            MultiplesAbonos pago = new MultiplesAbonos(getTransaccion());
            result = pago.doIt();
            doStatistic(pago);
        } else if (TipoTransaccion.ABONO_PENSIONADOS_FTP.equals(tipo)) {
            MultiplesAbonos pago = new MultiplesAbonosPlanilla(getTransaccion());
            result = pago.doIt();
            doStatistic(pago);
        } else if (TipoTransaccion.PAGADURIAS.equals(tipo)) {
        	 MultiplesAbonos pago = new MultiplesAbonosPlanilla(getTransaccion());
             result = pago.doIt();
             doStatistic(pago);
        } else if (TipoTransaccion.PAGO_PLANILLA.equals(tipo)) {
            MultiplesAbonos pago = null;
            if (bancaPersonas) {
            	pago = new MultiplesAbonos(getTransaccion());
            } else {
            	pago = new MultiplesAbonosPlanilla(getTransaccion());
            }
            result = pago.doIt();
            doStatistic(pago);
        } else if (TipoTransaccion.PAGO_PLANILLA_BONIFICACION.equals(tipo)) {
            MultiplesAbonos pago = null;
            if (bancaPersonas) {
            	pago = new MultiplesAbonos(getTransaccion());
            } else {
            	pago = new MultiplesAbonosPlanilla(getTransaccion());
            }
            result = pago.doIt();
            doStatistic(pago);
        }else if (TipoTransaccion.PAGO_PLANILLA_SALARIO_EDUCO.equals(tipo)) {
            MultiplesAbonos pago = new MultiplesAbonosPlanillaEduco(getTransaccion());
            result = pago.doIt();
            doStatistic(pago);
        } else if (TipoTransaccion.PAGO_PLANILLA_ISSS_EDUCO.equals(tipo)) {
            MultiplesAbonos pago = new MultiplesAbonosPlanillaIsssEduco(getTransaccion());
            result = pago.doIt();
            doStatistic(pago);
        } else if (TipoTransaccion.PAGO_PROVEEDORES.equals(tipo) || TipoTransaccion.LIQUIDACION_FACTURACION_POS.equals(tipo)) {
            MultiplesAbonos pago = null;
            if (bancaPersonas) {
            	pago = new MultiplesAbonos(getTransaccion());
            } else {
            	pago = new MultiplesAbonosPlanilla(getTransaccion());
            }
            result = pago.doIt();
            doStatistic(pago);
        } else if(TipoTransaccion.PAGO_MULTIPLE_ISSS.equals(tipo)) {
            MultiplesAbonos pago = new MultiplesAbonos(getTransaccion());
            result = pago.doIt();
            doStatistic(pago);
        } else if (TipoTransaccion.PAGO_IMPUESTOS_HACIENDA.equals(tipo)) {
            PagoHacienda pago = new PagoHacienda(getTransaccion());
            result = pago.doIt();
            doStatistic(pago);
        } else if (TipoTransaccion.SOLICITUD_CHEQUERAS.equals(tipo)) {
            SolicitudChequera solicitud = new SolicitudChequera(getTransaccion());
            result = solicitud.doIt();
            doStatistic(solicitud);
        } else if (TipoTransaccion.SOLICITUD_TRANSFERENCIA_INTERNACIONAL.equals(tipo)) {
        	
        	LineaTransaccion l = (LineaTransaccion) getTransaccion().getLineaTransaccionList().get(0);
        	String fechaVencimiento = l.getNumeroComprobante();
        	if(fechaVencimiento!=null && !fechaVencimiento.equals(" ") && fechaVencimiento.length()>0){
	        	System.out.println("fecha Vencimiento: "+fechaVencimiento);
	        	System.out.println("cantidad :"+l.getMonto());
	        	SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
	        	Date date1 = df.parse(fechaVencimiento);
	        	Date date2 = new Date(System.currentTimeMillis());
	        	 System.out.println(date1.toString());
	        	 System.out.println(date2.toString());
	        	 
        
	        	 if(date2.after(date1)){//Rechazar transaccion por ticket vencido
	        		 System.out.println("hoy :"+date2+" es despues de: "+date1);
	        		 result = MQMensajeDevolucion.TRADETICK_VENCIDO;
	        		 l.setResultado(MQResultado.NO_PROCESADO);
	             	 l.setMensajeDevolucion(result);
	             	 HibernateMap.update(l);
	        		 
	        	 }
        	}
        	
		            //realiza la reserva de fondos
		            boolean realizarReserva = realizarReserva();
		            if(realizarReserva){
		                Reserva reserva = new Reserva(getTransaccion());
		                result = reserva.doIt();
		                doStatistic(reserva);
		            }
		            if((realizarReserva && result == 0) || (!realizarReserva && result == MQResultado.NO_PROCESADO)){
		                TransferenciaInternacional solicitud = new TransferenciaInternacional(getTransaccion());
		                result = solicitud.doIt();
		                doStatistic(solicitud);
		                System.out.println("RESULTADO DE TRANSFERENCIA INTERNACIONAL: " + result); 
		                if(result == 0){
		                	//Actualizo linea de impuesto si existe
		                    LineaTransaccion lineaImpuesto = null;
		                    Iterator iter                  = getTransaccion().getLineaTransaccionList().iterator();
		                    while(iter.hasNext()){
		                        lineaImpuesto = (LineaTransaccion)iter.next();
		                        if(lineaImpuesto.getFlags() == TransaccionFlags.IMPUESTO)
		                            break;
		                    }
		                    if(lineaImpuesto != null){
		                        lineaImpuesto.setResultado(0);
		                        lineaImpuesto.setMensajeDevolucion(0);
		                        HibernateMap.update(lineaImpuesto);
		                    }
		                    try{
		                    	System.out.println("RESULTADO EXITOSO ENVIANDO CORREO"); 
		                        enviarCorreoTransferenciaInternacional(new SolicitudTransferenciaInternacional(),realizarReserva);
		                    }catch(Exception e){
		                        e.printStackTrace();
		                    }
		                }
		                try{
			              
			                SolicitudTransferenciaInternacional sol = new SolicitudTransferenciaInternacional();
			                sol.fromBytes(l.getMemoAsBytes());
			             Pagina2DatosSolicitante p = (Pagina2DatosSolicitante) sol.getPagina2();
			               System.out.println("Se notificara este TRADETICKET: "+p.getSeleccionado());
			               Servicios.notificacionTradeTicket(p.getSeleccionado());
		                }catch(Exception e){
		                	e.printStackTrace();
		                }
		                
		            }
         
        		
        } else if (TipoTransaccion.SOLICITUD_TARJETAS.equals(tipo)) {
            enviarCorreo(new SolicitudTarjetaCredito());
            result = 0;
        } else if (TipoTransaccion.SOLICITUD_CHEQUE_CAJA.equals(tipo)) {
            enviarCorreo(new SolicitudChequeCaja());
            result = 0;
        } else if (TipoTransaccion.SOLICITUD_DESEMBOLSO_LINEA_CREDITO.equals(tipo)) {
            enviarCorreo(new SolicitudDesembolsos());
            result = 0;
        } else if (TipoTransaccion.SOLICITUD_ENMIENDA_CARTA_CREDITO.equals(tipo)) {
            enviarCorreo(new SolicitudEnmiendaCartaCredito());
            result = 0;
        } else if (TipoTransaccion.SOLICITUD_PRESTAMO.equals(tipo)) {
            enviarCorreo(new SolicitudPrestamo());
            result = 0;
        } else if (TipoTransaccion.SOLICITUD_APERTURA_CUENTA_AHORRO.equals(tipo)) {
            enviarCorreo(new SolicitudCuentaAhorro());
            result = 0;
        } else if (TipoTransaccion.SOLICITUD_APERTURA_CUENTA_CORRIENTE.equals(tipo)) {
            enviarCorreo(new SolicitudCuentaCorriente());
            result = 0;
        } else if (TipoTransaccion.SOLICITUD_APERTURA_CUENTA_DEPOSITO_PLAZO.equals(tipo)) {
            enviarCorreo(new SolicitudDepositoPlazo());
            result = 0;
        } else if (TipoTransaccion.SOLICITUD_CARTA_CREDITO.equals(tipo)) {
            doSolicitudCartaCredito(new SolicitudCartaCredito());
            result = 0;
        } else if (TipoTransaccion.PAGO_AFP.equals(tipo)){
            PagoAfp pagoAfp = new PagoAfp(getTransaccion());
            result = pagoAfp.doIt();
            doStatistic(pagoAfp);
        } else if(TipoTransaccion.RESERVA.equals(tipo)){
            Reserva reserva = new Reserva(getTransaccion());
            LineaTransaccion linea = getTransaccion().getLineaTransaccion(0);
            linea.setCuenta(Comprobante.loadCuentaFromDb(linea.getCuentaAsString()));
            if(linea.getCuenta().esTarjetaCredito()){
                result = reserva.doIt();
            }else{
                result = reserva.doItPagoes();
            }
            doStatistic(reserva);
		} else if(TipoTransaccion.PAGO_CENTREX.equals(tipo)){
            PagoCENTREX centrex = new PagoCENTREX(getTransaccion());
            result = centrex.doIt();
            doStatistic(centrex);
        } else if(TipoTransaccion.PAGO_RESERVACION_TACA.equals(tipo)){
            PagoReservacionTACA reservaTaca = new PagoReservacionTACA(getTransaccion());
            result = reservaTaca.doIt();
            doStatistic(reservaTaca);
        }else if (TipoTransaccion.TRANSFERENCIA_CUENTAS_TELETON.equals(tipo)) {
            TransferenciaTercero transferencia = new TransferenciaTercero(getTransaccion());
            result = transferencia.doIt();
            doStatistic(transferencia);
        }else if (TipoTransaccion.RECARGA_CELULAR.equals(tipo)) {
            RecargaCelular recargaCel = new RecargaCelular(getTransaccion());
            result = recargaCel.doIt();
            doStatistic(recargaCel);
        }else if(TipoTransaccion.CARGA_QUEDANS.equals(tipo)){
        	CargaQuedanTransaction cargaQuedanExecuter = new CargaQuedanTransaction(getTransaccion());
        	cargaQuedanExecuter.execute();
        	Multiples mul = cargaQuedanExecuter.compatPotalaGenerateMultiples();
        	result =mul.getResultado();        	
            doStatistic(mul);
        }else if(TipoTransaccion.AUTORIZAR_REIMP_CHEQUES.equals(tipo)){
        	ChequesVencidosTransaction trans = new ChequesVencidosTransaction(getTransaccion());
        	trans.execute();
        	Multiples mul = trans.compatPotalaGenerateMultiples();
        	result = mul.getResultado();        	
            doStatistic(mul);
        }else if(TipoTransaccion.PAGO_AES.equals(tipo)){
            PagoAES trans = new PagoAES(getTransaccion());
            result = trans.doIt();
            doStatistic(trans);
        }else if (TipoTransaccion.SOLICITUD_AFILIACION_TRX_AUTOMATICA.equals(tipo)) {
        	result = doSolicitudAfiliacionTrxAutomatica(new SolicitudAfiliacionTrxAutomaticas());
        } else if (TipoTransaccion.CAMBIO_MONTO_CHEQUES.equals(tipo)) {
        	result = doSaveMontoCheque();
        } else if (TipoTransaccion.CARGA_BOLETA_PAGO.equals(tipo)) {
        	AplicadorBoletaPago pago = new AplicadorBoletaPago(getTransaccion());
            result = pago.doIt();
            doStatistic(pago);
        }else if (TipoTransaccion.PUBLICACION_DOCUMENTOS_AE.equals(tipo)) {
        	MultiplesAbonos publicaDocumentos = new MultiplesPublicacionDocumentosAE(getTransaccion());
            result 	= publicaDocumentos.doIt();
            doStatistic(publicaDocumentos);
        }else if (TipoTransaccion.ANTICIPO_DOCUMENTOS.equals(tipo)) {
        	MultiplesAbonos publicaDocumentos = new MultiplesAnticipoDocumentosAED(getTransaccion());
            result 	= publicaDocumentos.doIt();
            doStatistic(publicaDocumentos);
        }else if (TipoTransaccion.PAGO_PROVEEDORES_AED.equals(tipo)) {
        	MultiplesAbonos pago = new MultiplesPagosProveedoresAED(getTransaccion());
        	//MultiplesAbonos pago = new MultiplesPagosPlanillaServicios(getTransaccion());
            result = pago.doIt();
            doStatistic(pago);
    	}else if (TipoTransaccion.SOLICITUD_LC_PROVEEDOR.equals(tipo)) {
    		result = doSolicitarLineaCredito();
        }else if (TipoTransaccion.RECOMENDAR_PROVEEDOR.equals(tipo)) {
        	result = doRecomendacionProveedor();
        }else if (TipoTransaccion.TRANSF_PROPIAS_ACH.equals(tipo) ||
        		TipoTransaccion.TRANSF_TERCEROS_ACH.equals(tipo)  ||
        		TipoTransaccion.PAGO_TARJETA_ACH.equals(tipo) 	  ||
        		TipoTransaccion.PAGO_PRESTAMO_ACH.equals(tipo)    ||
        		TipoTransaccion.PAGO_PLANILLA_ACH.equals(tipo)    || 
        		TipoTransaccion.PAGO_PROVEEDORES_ACH.equals(tipo) ||
        		TipoTransaccion.PAGADURIAS_ACH.equals(tipo)       ||
        		TipoTransaccion.PAGO_DE_PENSIONES_ACH.equals(tipo) ){
        	MultiplesAbonos pago = new MultipleAbonoNoPropioACH(getTransaccion());
        	doSolicitudTransferenciaACH();
            result = pago.doIt();
            doStatistic(pago);
    	}else if (TipoTransaccion.PRE_PUBLICACION_DOCUMENTOS.equals(tipo)){
        	MultiplesAbonos prepublicaDocumentos = new MultiplesPrePublicacionDocumentosPR(getTransaccion());
            result 	= prepublicaDocumentos.doIt();
            doStatistic(prepublicaDocumentos);
        }else if (TipoTransaccion.AUTORIZAR_SOLICITUD_OID.equals(tipo)) {//---COMUNICACION CON MODULO DE PEGASO-----
        	EstadoSolicitudNegocio estadoSolicitudNegocio = (EstadoSolicitudNegocio) PegasoResolusor.getSingleton().getLocMultiNegocio().getServicio("estadoSolicitudNegocio");
            result 	= estadoSolicitudNegocio.ejecutarAutorizacionAplicada(getTransaccion(), mUsuario);
        }else if (TipoTransaccion.PAGO_SUBSIDIO.equals(tipo)){
        	MultiplesAbonos pago = null;
        	pago = new MultiplesAbonosPlanilla(getTransaccion());
        	result = pago.doIt();
        	doStatistic(pago);
        }else if (TipoTransaccion.PAGO_LEASING.equals(tipo)){
        	boolean referenciaEncontrada = false;
        	/** Validacion para transacciones programadas **/
        	
    		long horaInicioDiaMes    = Utilitarios.getPrimerDiaDelMes();
    		long horaFinDiaMes       = Utilitarios.getUltimoDiaDelMes();		
    		String referenciaLeasing = "";
    		
    		LineaTransaccion linea   = null;
    		Iterator iterLna         = getTransaccion().getLineaTransaccionList().iterator();;
    		while(iterLna.hasNext()){
				linea = (LineaTransaccion)iterLna.next();
				if(linea.esCredito()){
					referenciaLeasing = linea.getCuentaAsString();
				}
    		}
    		
    		List transacciones       = HibernateMap.queryAllDepths(TransaccionHistorica.class," where v.tipoTransaccion = 79 and v.estado = 80 and v.fechaUltimaModificacion >=" + horaInicioDiaMes + " and v.fechaUltimaModificacion <=" + horaFinDiaMes  + " and v.cliente = " + getTransaccion().getCliente().getId());
    		LineaTransaccion linea2   = null;
    		TransaccionHistorica trx = null;
    		
    		Iterator iterTrx;
    		Iterator iterLna2;
    		if(transacciones != null && transacciones.size() > 0){
    			iterTrx = transacciones.iterator();
    			while(iterTrx.hasNext() && referenciaEncontrada == false){
    				trx = (TransaccionHistorica)iterTrx.next();
    				if(trx != null){
    					iterLna2 = trx.getLineaTransaccionList().iterator();
    					while(iterLna2.hasNext()){
    						linea2 = (LineaTransaccion)iterLna2.next();
    						if(linea2.esCredito()){
    							if(referenciaLeasing.trim().equals(linea2.getCuentaAsString().trim())){
    								referenciaEncontrada = true;
    								break;
    							}
    						}
    					}
    				}
    			}
    		}
               	
        	if(referenciaEncontrada){
        		result =  MQResultado.NO_PROCESADO;
        		List lineas = getTransaccion().getLineaTransaccionList();
	    	    LineaTransaccion linea3;
	    	    for (int i = 0; i < lineas.size(); i++){
	    	    	linea3 = (LineaTransaccion) lineas.get(i);
	    	       linea3.setResultado(MQResultado.NO_PROCESADO);
	    	       if(linea3.esCredito())	    	    	   
	    	    	   linea3.setMensajeDevolucion(MQMensajeDevolucion.PAGO_LEASING_YA_EFECTUADO);
	    	       else	    	       
	    	    	   linea3.setMensajeDevolucion(MQMensajeDevolucion.NO_APLICADO);
	    	       HibernateMap.update(linea3);
	    	    }        		
        	}else{
        		PagoPrestamo pago = new PagoPrestamo(getTransaccion());
                result = pago.doIt();            
                doStatistic(pago);	
        	}
            
        }else if (TipoTransaccion.ADICION_CUENTAS_PREDEFINIDAS.equals(tipo)) {
        	//verifica si es adicion de cuenta en lotes.
        	List lineaTransaccionList = transaccion.getLineaTransaccionList();
        	if(lineaTransaccionList.size()>1){
        		//en lote
        		MultiplesCuentasPredefinidas multiplesCtoPredefinidas = new MultiplesCuentasPredefinidas(transaccion);
        		result = multiplesCtoPredefinidas.doIt(com.ba.potala.util.Constantes.ADICION_CUENTAS_PREDEFINIDAS);
        	}else{
        		result = doCuentasPredefinidas(com.ba.potala.util.Constantes.ADICION_CUENTAS_PREDEFINIDAS);
        	}
        }else if (TipoTransaccion.ELIMINAR_CUENTAS_PREDEFINIDAS.equals(tipo)) {
//        	verifica si es adicion de cuenta en lotes.
        	List lineaTransaccionList = transaccion.getLineaTransaccionList();
        	if(lineaTransaccionList.size()>1){
        		//en lote
        		MultiplesCuentasPredefinidas multiplesCtoPredefinidas = new MultiplesCuentasPredefinidas(transaccion);
        		result = multiplesCtoPredefinidas.doIt(com.ba.potala.util.Constantes.ELIMINACION_CUENTAS_PREDEFINIDAS);
        	}else{
        		result = doCuentasPredefinidas(com.ba.potala.util.Constantes.ELIMINACION_CUENTAS_PREDEFINIDAS);
        	}
        }else if(TipoTransaccion.DESEMBOLSO_CREDIPOS.equals(tipo)){
        	result = doDesembolsoCrediPOS();
        }else if (TipoTransaccion.DATOS_USUARIO.equals(tipo)){
        	DatosUsuario datosUsuario = new DatosUsuario(getTransaccion());
        	result = datosUsuario.doIt();   
        	//cpalacios; 20160607; auditoriaUsuarios
        	//dejamos marca de auditoria
        	
        	//request.getRemoteAddr();
        	datosUsuario.auditoriaDatosUsuario(result);
        	
        }else if(TipoTransaccion.LIBERACION_DE_FONDOS.equals(tipo)){
            LiberacionFondos liberacionFondos = new LiberacionFondos(transaccion);
            result = liberacionFondos.doIt();
            doStatistic(liberacionFondos);
        }else if(TipoTransaccion.CONFIRMACION_CHEQUERA.equals(tipo)){
            ConfirmacionChequera confirmacionChequera = new ConfirmacionChequera(transaccion);
            result = confirmacionChequera.doIt();
            doStatistic(confirmacionChequera);
        }else if (TipoTransaccion.IDENTIFICACION_DEPOSITOS.equals(tipo)) {
        	MultiplesIdentificacionDepositos identificacionDepositos = new MultiplesIdentificacionDepositos(getTransaccion());
            result 	= identificacionDepositos.doIt();
        }else if (TipoTransaccion.ELIMINACION_CODIGOS_IDENTIF.equals(tipo)) {
        	EliminacionCodigosIdentif eliminacionCodigosIdentif = new EliminacionCodigosIdentif(transaccion);
            result 	= eliminacionCodigosIdentif.doIt();
        }else if (TipoTransaccion.COTIZACION_DE_MONEDA_EN_LINEA.equals(tipo)) {
        	CotizacionMonedaEnLinea cotizacionMonedaEnLinea = new CotizacionMonedaEnLinea(transaccion);
            result 	= cotizacionMonedaEnLinea.doIt();
            if (result==0) {
            	enviarCorreoCotizacionMoneda();
            }
        }else if (TipoTransaccion.VERIFICACION_CUENTAS.equals(tipo)) {
        	//cpalacios;201610
            System.out.println("VERIFICADOR DE CUENTAS DE PLANILLAS - OCTUBRE2016");
            //tomando ejemplo abono planillas
            MultiplesAbonos pago = null;

            pago = new MultiplesVerificaCuentasPlanilla(getTransaccion());
            result = pago.doIt(); 
            
        }else if(TipoTransaccion.DESEMBOLSO_EN_LINEA_LC.equals(tipo)){
        	result = doDesembolsoLineasCredito();
        }else{
            throw new Exception("Tipo de Transaccion " + tipo + " NO tiene accion definida");
        }
        
        return result;
    }
    
	/**
     *
     * @param flsThread
     * @return
     * @throws Exception
     */
    public int execute() throws Exception {
        // Forzando que la transaccion se ejecte solo una vez, no hay excepciones
        int result = MQResultado.NO_PROCESADO;
        try {
            result = doTransaction();
        } catch (Exception e) {
            System.out.println("\n\n\n\nHubo un error en " + this.getClass());
            System.out.println("Datos de la transaccion = " + getTransaccion());
            System.out.println("Tipo de transaccion a ejecutar = " + getTransaccion().getTipoTransaccion() + "\n\n\n\n");
            e.printStackTrace();
            result = MQResultado.EXCEPTION;
        }
        getEstadistica().setFechaTerminoAplicacion(System.currentTimeMillis());
        getTransaccion().setMemoExtraAsBytes(TransaccionUtils.putEstadisticaToExtra(getTransaccion().getMemoExtraAsBytes(), getEstadistica()));
        getTransaccion().setEstado((result == MQResultado.APLICADO_MQ) ? EstadoTransaccion.ESTADO_APLICADA : EstadoTransaccion.ESTADO_RECHAZADA);
        
        HibernateMap.update(getTransaccion());
        TransaccionUtils.pasarTransaccionAHistorica(getTransaccion());
        
        return result;
    }
    
    /**
     * Prepara los datos necesarios para la creacion de la solicitud de 
     * afiliacion a los cargos automaticos
     * @param solicitud
     * @throws Exception
     */
    private int doSolicitudAfiliacionTrxAutomatica(SolicitudAfiliacionTrxAutomaticas solicitud) throws Exception{
    	int result = MQResultado.NO_PROCESADO;
        getEstadistica().setCantidadAplicadoDebito(1);
        
        //----Todas las solicitudes tiene una sola linea de transaccion
        LineaTransaccion linea = (LineaTransaccion) getTransaccion().getLineaTransaccionList().get(0);
        solicitud.fromBytes(linea.getMemoAsBytes());
        
        //----Tomamos todas las paginas
        SolicitudAfiliacionTrxAutomaticaPagina1 pagina1 = (SolicitudAfiliacionTrxAutomaticaPagina1) solicitud.getPagina1();
        
        SolicitudAfiliacionTrxAutomaticaDB solicitudAfiliacionCargAutomDB = new SolicitudAfiliacionTrxAutomaticaDB();
        solicitudAfiliacionCargAutomDB.setCliente(""+solicitud.getCliente().getId());
        
        //----Pagina 1
        solicitudAfiliacionCargAutomDB.setEmail(pagina1.getEmail());
        solicitudAfiliacionCargAutomDB.setTelefonoNotific(pagina1.getTelefonoNotific());
        solicitudAfiliacionCargAutomDB.setCuentaCargo(pagina1.getCuentaCargar().getNumero());
        solicitudAfiliacionCargAutomDB.setCodigoConvenio(""+pagina1.getConvenioServicio().getCodigo());
        solicitudAfiliacionCargAutomDB.setNombreConvenio(""+pagina1.getConvenioServicio().getNombre());
        solicitudAfiliacionCargAutomDB.setReferenciaServicio(pagina1.getReferenciaServicio());
        
        String tipoCuentaAsCodigo = "";
        if(pagina1.getRadioTipoCuentaCargo().trim().equalsIgnoreCase("tarjetacredito")){
        	tipoCuentaAsCodigo = "1";
        }else if(pagina1.getRadioTipoCuentaCargo().trim().equalsIgnoreCase("tarjetadebito")){
        	tipoCuentaAsCodigo = "2";
        }else
        	tipoCuentaAsCodigo = "3";
        
        solicitudAfiliacionCargAutomDB.setTipoCuentaCargo(tipoCuentaAsCodigo);
        int app = Sistema.getPropertyAsInt(Constantes.ID_APP_PERSONAL);
		if(app == 2)
			solicitudAfiliacionCargAutomDB.setCanalAfiliacion(com.ba.potala.util.Constantes.EBANCA_PERSONAS);
		else
			solicitudAfiliacionCargAutomDB.setCanalAfiliacion(com.ba.potala.util.Constantes.EBANCA_EMPRESAS);
		
        String rspFromService = Servicios.mantenimientoAfiliacionClienteConvenio(solicitudAfiliacionCargAutomDB, com.ba.potala.util.Constantes.TIPO_OPERACION_ADD, 
        													"ebanca", getUsuario().getUser());
        
        if(rspFromService!=null && rspFromService.trim().equalsIgnoreCase(com.ba.potala.util.Constantes.RESP_OK)){
            //----Actualizamos el resultado de la linea
            setResultadoLineas();
            
        	//MAX_LENGHT = 255
        	String extra = "Afil. Convenio# "+ pagina1.getConvenioServicio().getCodigo() + " - " +pagina1.getConvenioServicio().getNombre() +", Ref: "+pagina1.getReferenciaServicio()+
        			" Cta# "+pagina1.getCuentaCargar().getNumero();
        	
        	
            Servicios.createAuditoria(getUsuario(), extra);
            
            
            result = MQResultado.APLICADO_MQ;
            
            //cpalacios; AuditoriaUsuarios; se crea auditoria para transaccion de auditoria de usuarios; 20160704
    		String ip="";
    		//ip = getTransaccion().getDescripcion()!= null?getTransaccion().getDescripcion():" ";
			byte[] ipSerialized = getTransaccion().getMemoAsBytes();
			ip = (String) Utils.readObject(ipSerialized);
			ip = ip!= null?ip:" ";
    		
    		System.out.println("IP REMOTA AUDITORIA: "+ip);
            Servicios.createAuditoriaAfilicacionCargosAutom(getUsuario(), extra, ip, result);
            
            //----Debemos actualizar el correo electronico, tanto en e-banca, como en el IBS
            if(solicitudAfiliacionCargAutomDB.getEmail()!=null && solicitudAfiliacionCargAutomDB.getEmail().trim().length()>0){
            	if(!solicitudAfiliacionCargAutomDB.getEmail().trim().equalsIgnoreCase(getUsuario().getUsuario().getEmail())){
            		//if(Servicios.actualizarEmailCliente(solicitudAfiliacionCargAutomDB.getEmail(),""+solicitud.getCliente().getId(),getUsuario().getUser())){
            			getUsuario().getUsuario().setEmail(solicitudAfiliacionCargAutomDB.getEmail());
            			HibernateMap.update(getUsuario().getUsuario());            			
            		//}
            	}
            }
        }else{
        	//Por el motivo que sea, si el servicio no pudo realizar
        	//la afiliacion, se debe rechazar la transaccion
        	result = MQResultado.NO_PROCESADO;
        	
        	if(rspFromService!=null && rspFromService.trim().equalsIgnoreCase("000020")){
        		setResultadoLineasRechazadas(MQMensajeDevolucion.MSG_SOLICITUD_AFILIACION_YA_EXISTE);
        	}
        	if (rspFromService!=null && rspFromService.trim().equalsIgnoreCase("000022")){
        		setResultadoLineasRechazadas(MQMensajeDevolucion.MSG_CUENTA_CLIENTE_INVALIDA);
        	}else{
        	setResultadoLineasRechazadas(MQMensajeDevolucion.MSG_SOLICITUD_AFILIACION_NO_PROCESADA);
        		}
        }
        
        return result;
    }
    
    /**
     * Invocacion a servicio que permite realizar la actualizacion del monto de un cheque ya confirmado
     * o por confirmar 
     * @return
     * @throws Exception
     */
    private int doSaveMontoCheque()	throws Exception{
    	
    	int result = MQResultado.NO_PROCESADO;
    	getEstadistica().setCantidadAplicadoCredito(1);
		
        //----Todas las solicitudes tiene una sola linea de transaccion     
		LineaTransaccion linea = (LineaTransaccion) getTransaccion().getLineaTransaccionList().get(0);
        String numeroCheque = linea.getReferencia();
        String numeroCuenta = linea.getCuentaAsString();
        String montoActual = null;
        String montoNuevo  = Double.toString(linea.getMonto());
        long cliente = getTransaccion().getCliente().getId();
        
        //----Traemos el saldo actual del cheque
       
	    RequestDTODinamico requestDto = new RequestDTODinamico();
		requestDto.setAccount(Helper.completeLeftWith(numeroCuenta, '0', 12));
		requestDto.setClient(Helper.completeLeftWith(cliente+ "", '0', 9));
		String user = null;
		if(getUsuario().getUsuarioAsString().length() > 10) {
			user = 	getUsuario().getUsuarioAsString().substring(0, 10);				
		} else {
			user = getUsuario().getUsuarioAsString();
		}
		requestDto.setUser(user);
		requestDto.setBousr(user);
		requestDto.setAgency(Sistema.getProperty("DATAQUEUE.SUCURSAL"));
		requestDto.setDatetime(Helper.getCurrentDateTime());
		requestDto.agregarParametro("chequeInicial", Helper.completeLeftWith(numeroCheque+"", '0', 9));
		requestDto.agregarParametro("chequeFinal", Helper.completeLeftWith(numeroCheque+ "", '0', 9));
		ResponseDTODinamico responseDto = null;
		try {
			responseDto = Servicios.getCheques(requestDto);
			responseDto.primerContenedor();
			responseDto.siguiente();
			montoActual = responseDto.obtenerString("monto");
		} catch(AtributoRequestException ae){
		    System.out.println("Error de Atributos : " + ae.getMessage());
		}
        try {
        	String rspFromService = Servicios.saveCheques(numeroCuenta, getUsuario().getUsuarioAsString(), numeroCheque, montoActual, montoNuevo);       	        	
			String extra = "Numero Cheque: "+ numeroCheque + 
        			", Monto Actual :"+ montoActual + ", Monto Nuevo:" + montoNuevo ;
			
			if (rspFromService!= null && rspFromService.trim().equalsIgnoreCase(com.ba.potala.util.Constantes.RESP_OK))	{
	            //----Actualizamos el resultado de la linea
	            setResultadoLineas();
	            Servicios.createAuditoria(getUsuario(), extra);
	            result = MQResultado.APLICADO_MQ;
	        }else {
	        	//Por el motivo que sea, si el servicio no pudo realizar
	        	//la actualizacion, se debe rechazar la transaccion
	        	result = MQResultado.NO_PROCESADO;
	        	if(rspFromService!=null && rspFromService.trim().equalsIgnoreCase("000038")){
	        		setResultadoLineasRechazadas(MQMensajeDevolucion.CUENTA_INVALIDA_CAMBIO_CHEQUE);
	        	}else if (rspFromService!=null && rspFromService.trim().equalsIgnoreCase("000039")){
	        		setResultadoLineasRechazadas(MQMensajeDevolucion.CUENTA_NO_CONFIRMACION_CAMBIO_CHEQUE);
	        	} else if (rspFromService!=null && rspFromService.trim().equalsIgnoreCase("000040")){
	        		setResultadoLineasRechazadas(MQMensajeDevolucion.NUMERO_CHEQUE_INVALIDO_CAMBIO_CHEQUE);
	        	} else if (rspFromService!=null && rspFromService.trim().equalsIgnoreCase("000041")){
	        		setResultadoLineasRechazadas(MQMensajeDevolucion.MONTO_ACTUAL_INVALIDO_CAMBIO_CHEQUE);
	        	} else if (rspFromService!=null && rspFromService.trim().equalsIgnoreCase("000042")){
	        		setResultadoLineasRechazadas(MQMensajeDevolucion.MONTO_NUEVO_INVALIDO);
	        	} else if (rspFromService!=null && rspFromService.trim().equalsIgnoreCase("000043")){
	        		setResultadoLineasRechazadas(MQMensajeDevolucion.MONTO_ACTUAL_Y_NUEVO_INVALIDO);
	        	} else if (rspFromService!=null && rspFromService.trim().equalsIgnoreCase("000044")){
	        		setResultadoLineasRechazadas(MQMensajeDevolucion.NO_REGISTRADO_PARA_CAMBIOS);
	        	} else if (rspFromService!=null && rspFromService.trim().equalsIgnoreCase("000045")){
	        		setResultadoLineasRechazadas(MQMensajeDevolucion.MONTO_ACTUAL_DIFERENTE_AL_DE_ARCHIVO);
	        	} else if (rspFromService!=null && rspFromService.trim().equalsIgnoreCase("000046")){
	        		setResultadoLineasRechazadas(MQMensajeDevolucion.MONTO_RECIBIDO_DIFERENTE_AL_DE_ARCHIVO);
	        	}else{
	        	setResultadoLineasRechazadas(MQMensajeDevolucion.MSG_SOLICITUD_AFILIACION_NO_PROCESADA);
	        	}	        	
	       }
    	}catch (Exception e) {
		    e.printStackTrace();
		}
		return result;
    }
    
    private int doCuentasPredefinidas(String tipoOperacion) throws Exception{
    	int result = MQResultado.NO_PROCESADO;
    	int cuentasProcesadas = 0;
    	int cuentasNoProcesadas = 0;
    	List detallesError = new LinkedList();
    	Transaccion transaccion = getTransaccion();
    	List lineaTransaccionList = transaccion.getLineaTransaccionList();
    	LineaTransaccion lineaTransaccion = (LineaTransaccion) lineaTransaccionList.get(0);
    	
    	Object[] obj = Utils.readObjects(transaccion.getMemoAsBytes());
    	
    	String tipoPlanilla = (String) obj[0];
    	String cuentaAsociada = (String) obj[1];
    	
    	String[] respuesta = Servicios.mantenimientoCuentasPredefinidas(tipoOperacion, transaccion.getCliente().getId(), tipoPlanilla,lineaTransaccion.getCuentaAsString(), lineaTransaccion.getTramaEntradaAsString(), lineaTransaccion.getMemoAsString(), cuentaAsociada,transaccion.getUsuario().getUser(), null);
    	if("000000".equals(respuesta[0])){
    		result = MQResultado.APLICADO_MQ;
    		String resultado = "";
			if("A".equals(tipoOperacion)){
				resultado = "La cuenta fue registrada exitosamente";
			}else{
				resultado = "La cuenta fue eliminada exitosamente";
			}
			lineaTransaccion.setResultado(MQResultado.APLICADO_MQ);
			lineaTransaccion.setMensajeDevolucion(MQMensajeDevolucion.APLICADA);
    		lineaTransaccion.setReferencia(resultado);
    		cuentasProcesadas++;
    	}else{
    		lineaTransaccion.setResultado(MQResultado.NO_PROCESADO);
    		lineaTransaccion.setMensajeDevolucion(MQMensajeDevolucion.NO_APLICADO);
    		lineaTransaccion.setReferencia(respuesta[1]);
    		detallesError.add("No Cuenta " + lineaTransaccion.getCuentaAsString() + ", " + respuesta[1]);
    		cuentasNoProcesadas++;
    	}
    	HibernateMap.update(lineaTransaccion);
    	TransaccionUtils.notificarMtoCtoPredefinidas(transaccion,cuentasProcesadas, cuentasNoProcesadas, detallesError);
    	return result;
    	
    }
    
    /**
     * Ejecuta servicio de desembolso de CrediPOS
     * @return
     * @throws Exception
     */
    private int doDesembolsoCrediPOS()	throws Exception{
    	int result = MQResultado.NO_PROCESADO;
    	getEstadistica().setCantidadAplicadoCredito(1);
    	List lineas = getTransaccion().getLineaTransaccionList();
	    LineaTransaccion linea = null;
	    for (int i = 0; i < lineas.size(); i++){
	    	linea = (LineaTransaccion) lineas.get(i);
	    	if(!linea.esCredito()){
	    		linea = null;
	    	}else{
	    		break;
	    	}
	    }
		try {			
	    	String extra = "";        	
	    	DatosDesembolsoCrediPOS dbPro  =  (DatosDesembolsoCrediPOS)DatosDesembolsoCrediPOS.readObject(linea.getMemoAsBytes());
	    	System.out.println("TransactionExecuter.doDesembolsoCrediPOS(), datos desembolso:");
	    	System.out.println("TransactionExecuter.doDesembolsoCrediPOS(), cliente: " + dbPro.getCliente() + "-" + Long.toString(dbPro.getCliente().longValue()));
	    	System.out.println("TransactionExecuter.doDesembolsoCrediPOS(), linea: " + dbPro.getCorrelativoLinea() + "-" + Integer.toString(dbPro.getCorrelativoLinea().intValue()));
	    	Respuesta rp = Servicios.desembolsoCrediPOS(getUsuario().getUsuarioAsString(), "0",
	    			dbPro.getCliente().longValue(), 
	    			dbPro.getCorrelativoLinea().intValue(), 
	    			linea.getCuentaAsString(), 
	    			linea.getMonto(), 
	    			dbPro.getTasa().doubleValue(), 
	    			dbPro.getReferencia());
	    	if(rp != null && rp.getListaContenedores().size() > 0){
 				rp.primerContenedor();
 				rp.siguiente();
 				String codigo = rp.obtenerString("codigo").trim();
 				String descripcion = rp.obtenerString("descripcion").trim();
 				if(codigo.equalsIgnoreCase("000000")){
 					Respuesta rp1 = rp.obtenerSubRespuesta("cuerpo");
 					if(rp1 != null && rp1.getListaContenedores().size() > 0){
 						rp1.primerContenedor();
 						rp1.siguiente();
 						String prestamo = rp1.obtenerString("numeroPrestamo")!=null?rp1.obtenerString("numeroPrestamo").trim():"0";
 						result = MQResultado.APLICADO_MQ;
 						linea.setResultado(MQResultado.APLICADO_MQ);
 						linea.setMensajeDevolucion(0);
 						linea.setTramaEntradaAsString("Numero de prestamo:"+prestamo);
 						HibernateMap.update(linea);
 					}
 				}else{
 					result = MQResultado.NO_PROCESADO;
 		    	    linea.setResultado(MQResultado.NO_PROCESADO);
 		    	    if("000010".equals(codigo)){
 		    	    	linea.setMensajeDevolucion(MQMensajeDevolucion.CONVENIO_INVALIDO);
 		    	    }else if("000011".equals(codigo)){//Cliente invalido
 		    	    	linea.setMensajeDevolucion(471);
 		    	    }else if("000019".equals(codigo)){//Cliente no existe
 		    	    	linea.setMensajeDevolucion(407);
 		    	    }else if("000046".equals(codigo)){//No existe numero de linea
 		    	    	linea.setMensajeDevolucion(MQMensajeDevolucion.LINEA_CREDITO_NOEXISTE);
 		    	    }else if("000047".equals(codigo)){//Cuenta no existe
 		    	    	linea.setMensajeDevolucion(21);
 		    	    }else if("000052".equals(codigo)){//Tasa de interes invalida
 		    	    	linea.setMensajeDevolucion(MQMensajeDevolucion.TASA_INTERES_INVALIDA);
 		    	    }else if("000090".equals(codigo)){//Tasa de interes invalida
 		    	    	linea.setMensajeDevolucion(MQMensajeDevolucion.DESEMBOLSO_NOPERMITIDO);
 		    	    }else if("000093".equals(codigo)){//Cuenta de deposito con restricción
 		    	    	linea.setMensajeDevolucion(MQMensajeDevolucion.CUENTA_CON_RESTRICCIONES);
 		    	    }else if("000120".equals(codigo)){//Sin disponible
 		    	    	linea.setMensajeDevolucion(158);
 		    	    }else if("000137".equals(codigo)){//Referencia de prestamo no existe
 		    	    	linea.setMensajeDevolucion(MQMensajeDevolucion.REFERENCIA_PRESTAMO_NOEXISTE);
 		    	    }else if("000152".equals(codigo)){//Linea de credito vencida
 		    	    	linea.setMensajeDevolucion(151);
 		    	    }else if("000153".equals(codigo)){//Cuenta a bonar con producto invalido
 		    	    	linea.setMensajeDevolucion(MQMensajeDevolucion.PRODUCTO_CUENTA_INVALIDO);
 		    	    }
 		    	    linea.setTramaEntradaAsString(codigo+":"+descripcion);
 		    	    HibernateMap.update(linea);
 				}
	    	}else{
	    		result = MQResultado.NO_PROCESADO;
	    	    linea.setResultado(MQResultado.NO_PROCESADO);
	    	    linea.setMensajeDevolucion(MQMensajeDevolucion.SERVICIO_TEMPORALMENTE_NO_DISPONIBLE);
	    	    linea.setTramaEntradaAsString("No hubo respuesta del servicio de desembolso CrediPOS");
	    	    HibernateMap.update(linea);
	    	}	    	
	        Servicios.createAuditoria(getUsuario(), extra);		
    	}catch (Exception e){
		    e.printStackTrace();
		}		
		return result;		
	}
    
    private int doRecomendacionProveedor()	throws Exception{
    	int result = MQResultado.NO_PROCESADO;
    	getEstadistica().setCantidadAplicadoCredito(1);
		try {			
	    	String extra = "";        	
	    	SolicitudIncorporacionProveedorDB dbPro  =  (SolicitudIncorporacionProveedorDB)SolicitudIncorporacionProveedorDB.readObject(getTransaccion().getMemoAsBytes());        	
	    	String respuesta = Servicios.recomendarProveedor(dbPro.getUsuarioAsString(),
	    			dbPro.getIdConvenio(), 
	    			dbPro.getNit(), 
	    			dbPro.getNombreSeguntNit(), 
	    			dbPro.getNombreProveedor(), 
	    			dbPro.getContactoEmpresa(), 
	    			dbPro.getMailDeContacto(), 
	    			dbPro.getTelefonoDeContacto(), 
	    			dbPro.getTiempoRelacionComercial(), 
					dbPro.getPlazoPagoProveedor(), 
					String.valueOf(Utils.redondeo2Decimales((Double.parseDouble(dbPro.getPagoPromedioMensual())))), 
					dbPro.getFlagEBanca(), 
					dbPro.getFlagMora(),
					dbPro.getMora30Dias(),
					dbPro.getMora60Dias(),
					dbPro.getMora90Dias());

	    	String respuestaArray[] = respuesta.split("/");
	    	if(dbPro.getFlagEBanca().equals("Y") && respuestaArray[0].equals("000000")){

	        	try {
	        		Conector c = Entorno.e().obtenerConector("conectorEBANCA");
	        		Peticion p = new Peticion();
	        		
	        		p.agregarParametro("cliente",dbPro.getNumeroCliente());
	        		p.agregarParametro("mensaje"," Ha sido recomendando por " + getTransaccion().getUsuario().getCliente().getNombre() + " para ser relacionado a su circulo de proveedores, " +
					"usted puede ahora SOLICITAR UNA LÍNEA DE CRÉDITO PARA FINANCIAR SUS DOCUMENTOS DE VENTA");
	        		
	        		Respuesta rp = c.obtenerDatos(p,null,"guardarMensajeParaCliente",null);
	        		
	        		if(rp!=null){
	        			rp.primerContenedor();
	        			if(rp.siguiente()){
	        				if(rp.obtenerString("actualizacion")!=null && rp.obtenerString("actualizacion").equalsIgnoreCase("1")){
	        					//Se guardo el mensaje OK
	        					System.out.println("mensaje enviado OK");
	        				} else {
	        					System.err.println("Error enviando mensaje");
	        				}
	        			}
	        		}
	        	}catch (Exception e){
	        		e.printStackTrace();
	        	}
	        	
	        	
			}
	    	
	    	if(respuestaArray.length == 1 || respuestaArray.length == 0){
	    		result = MQResultado.NO_PROCESADO;
	    		List lineas = getTransaccion().getLineaTransaccionList();
	    	    LineaTransaccion linea;
	    	    for (int i = 0; i < lineas.size(); i++){
	    	       linea = (LineaTransaccion) lineas.get(i);
	    	       linea.setResultado(MQResultado.NO_PROCESADO);
	    	       linea.setMensajeDevolucion(MQMensajeDevolucion.NO_APLICADO);
	    	       HibernateMap.update(linea);
	    	    }
	    	}
	    	
	    	if(respuestaArray.length == 2 && !respuestaArray[0].equals("000000")){
	    		result = MQResultado.NO_PROCESADO;
	    		List lineas = getTransaccion().getLineaTransaccionList();
	    	    LineaTransaccion linea;
	    	    for (int i = 0; i < lineas.size(); i++){
	    	       linea = (LineaTransaccion) lineas.get(i);
	    	       linea.setResultado(MQResultado.NO_PROCESADO);
	    	       linea.setMensajeDevolucion(MQMensajeDevolucion.NO_APLICADO);
	    	       HibernateMap.update(linea);
	    	    }
			}else{
				result = MQResultado.APLICADO_MQ;
	    		List lineas = getTransaccion().getLineaTransaccionList();
	    	    LineaTransaccion linea;
	    	    for (int i = 0; i < lineas.size(); i++){
	    	       linea = (LineaTransaccion) lineas.get(i);
	    	       linea.setResultado(MQResultado.APLICADO_MQ);
	    	       linea.setMensajeDevolucion(Integer.parseInt(respuestaArray[0]));
	    	       HibernateMap.update(linea);
	    	    }
	    	    
//	    	  Se envía un correo de notificación a colaboradores BA y ejecutivo asignad
	        	try{
	        		String emailEjecutivo = "";
	        		String[] to;
	        		String subject;
	        		String from;
	        		String[] message;
	        		Mailer servidorCorreo = new Mailer();
	        		
	        		//Primero se obtiene el email del ejecutivo asignado si acaso tiene
	        		emailEjecutivo = Servicios.obtenerEmailEjecutivoAsignado(getTransaccion().getUsuario(),dbPro.getNumeroCliente());
	        		
	        		if(emailEjecutivo!=null && !emailEjecutivo.equals("")){
	        			System.out.println("*****Email del Ejecutivo******: "+emailEjecutivo);
	        			//emailEjecutivo = "jaegonza@bancoagricola.com.sv";
	        			emailEjecutivo = emailEjecutivo.trim();
	        			to = new String[] {"gpactivo@bancoagricola.com.sv",emailEjecutivo};	        			
	        		}else{
	        			emailEjecutivo="";
	        			to = new String[] {"gpactivo@bancoagricola.com.sv"};	        				        			
	        		}
	        		
	        		subject = "Recomendación de Proveedor E-Anticipo Empresarial.";
	        		from = null;
	        		message = new String[] {"Estimado Usuario:<br/><br/>","El cliente <b>"+dbPro.getNombreSeguntNit()+"</b> con número único <b>"+ dbPro.getNumeroCliente() +
	        		"</b> ha sido recomendado por su Agente Empresarial <b>"+getTransaccion().getUsuario().getCliente().getNombre()+"</b> para optar a una línea de e-anticipo Empresarial. " +
	        		"<br/><br/>A partir de este momento se puede iniciar la gestión comercial de la operación. Para mayor información puede localizar esta solicitud en la opción en PORTAL en " +
	        		"la siguiente ruta de acceso: Anticipo Empresarial > Admin. e-anticipo Empresarial > Consulta Convenios AE. En horario hábil del siguiente día."};
	        		
	        		servidorCorreo.sendMail(to, subject, from, message);
	        	}catch (Exception e){
	        		e.printStackTrace();
	        	}
			}
	        Servicios.createAuditoria(getUsuario(), extra);		
    	}catch (Exception e){
		    e.printStackTrace();
		}		
		return result;		
	}
    
    private int doSolicitarLineaCredito()	throws Exception{
    	int result = MQResultado.NO_PROCESADO;
    	getEstadistica().setCantidadAplicadoCredito(1);

		try {
        	String extra = "Numero Gestion : ";
            Servicios.createAuditoria(getUsuario(), extra);
            result = MQResultado.APLICADO_MQ;
            setResultadoLineas();
		}catch (Exception e) {
		    e.printStackTrace();
		}
		return result;
	}
    
    private int doSolicitudTransferenciaACH() throws Exception{
    	int result = MQResultado.NO_PROCESADO;
    	getEstadistica().setCantidadAplicadoCredito(1);
		String idTransaccion = Long.toString(getTransaccion().getId());
		ObservacionPlanillas observacion = new ObservacionPlanillas();
		
		List listaLineas				=	getTransaccion().getLineaTransaccionList();			
		Iterator listaLinea				=	listaLineas.iterator();
		while(listaLinea.hasNext()){
			LineaTransaccion linea		=	(LineaTransaccion)listaLinea.next();
			if (linea.esCredito()){
				
				observacion.fromBytes(linea.getMemoAsBytes());
				SolicitudTransferenciaACHDB dbAux	=	(SolicitudTransferenciaACHDB)SolicitudTransferenciaACHDB.readObject(observacion.getObservacion().getBytes());
				
	        	String extra = "Numero Gestion : " + dbAux.getTipoTransaccionACH() +", Banco : " + dbAux.getNombreBancoDestino()+
    			", Tipo Cuenta:" + dbAux.getTipoCuentaACH();
	        	
	        	Servicios.createAuditoria(getUsuario(), extra);

	            OperacionesACHSQL operacionACH = new OperacionesACHSQL(dbAux);
	            operacionACH.save(idTransaccion, Long.toString(linea.getId()));

			}	
		}
		return result;
    }
    
    private boolean esColectorAes(Transaccion transaccion) {
    	boolean colectorAES = false;
    	Iterator iter = transaccion.getLineaTransaccionList().iterator();
    	//System.out.println("TransactionExecuter.esColectorAes(), getLineaTransaccionList() = " + transaccion.getLineaTransaccionList().size());
    	String referencia = null;
    	while(iter.hasNext()){
    		LineaTransaccion linea = (LineaTransaccion) iter.next();
    		//System.out.println("TransactionExecuter.esColectorAes(), linea = " + linea.getTipo());
    		if(linea.getTipo().esCredito()){
    			referencia = linea.getReferencia();
    			break;
    		}
    	}
    	//System.out.println("TransactionExecuter.esColectorAes(), referencia = " + referencia);
    	if(referencia != null && referencia.length() >= 4){
    		String colector = referencia.substring(0, 4);
    		//CAESS = 1898, CLESA = 2260, EEO = 2253, DEUSEM = 1881
    		if(colector.equals("1898") || colector.equals("2260") || colector.equals("2253") || colector.equals("1881")){
    			colectorAES = true;
    		}
    	}
		return colectorAES;
	}
    
    /**
     * Verifica si la transferencia es en dolares para realizar la reserva, en caso contrario no la realiza 
     * @return
     * @throws Exception 
     */
    private boolean realizarReserva() throws Exception{
        boolean esDolar = false;
        Transaccion transaccion = getTransaccion();
        LineaTransaccion lineaTransaccion = null;
        
        Iterator iter = transaccion.getLineaTransaccionList().iterator();
        while(iter.hasNext()){
        	lineaTransaccion = (LineaTransaccion)iter.next();
            if(lineaTransaccion.getFlags() == TransaccionFlags.NORMAL)
                break;
        }
        
        
        SolicitudTransferenciaInternacionalForm solicitudForm = new SolicitudTransferenciaInternacionalForm();
        solicitudForm.fromBytes(lineaTransaccion.getMemoAsBytes());
        Pagina3DatosTransaccion datosTransaccion = (Pagina3DatosTransaccion) solicitudForm.getSolicitud().getPagina3();
        Moneda moneda = datosTransaccion.getMoneda();
        if(moneda != null && moneda.getId() == 1){
            esDolar = true;
        }
        return esDolar;
    }

//desembolsos para lineas de credito fijas y rotativas
    private int doDesembolsoLineasCredito()	throws Exception{
    	int result = MQResultado.NO_PROCESADO;
    	getEstadistica().setCantidadAplicadoCredito(1);
    	List lineas = getTransaccion().getLineaTransaccionList();
	    LineaTransaccion linea = null;
	    for (int i = 0; i < lineas.size(); i++){
	    	linea = (LineaTransaccion) lineas.get(i);
	    	if(!linea.esCredito()){
	    		linea = null;
	    	}else{
	    		break;
	    	}
	    }
	    if(linea == null)
	    	System.out.println("La linea viene vacia");
	    	
		try {			
	    	String extra = "";
	    	String xmlPreview = new String(linea.getMemoAsBytes());
	    	System.out.println("---> El XML es : [[ "+ xmlPreview +" ]]");
	    	DatosConvenioDesembLC dbPro  =  (DatosConvenioDesembLC)DatosConvenioDesembLC.readObject(linea.getMemoAsBytes());
	    	System.out.println("TransactionExecuter.doDesembolsoLineasCredito(), datos desembolso:");
	    	System.out.println("TransactionExecuter.doDesembolsoLineasCredito(), cliente: " + dbPro.getClienteLinea());
	    	System.out.println("TransactionExecuter.doDesembolsoLineasCredito(), linea: " + dbPro.getNumeroLinea());
	    	
	    	//double cuota =0;
			BigDecimal cuota = new BigDecimal(0);
	    	//cuota = dbPro.getCuotaDesembolso().doubleValue();
	    	
	    	String tipoLineaCred = "";
	    	tipoLineaCred = dbPro.getTipoLinea();
	    	
			String frecuenciaPago = "";
			frecuenciaPago = dbPro.getCicloPagoCapital();
			String tipoPlazo = "Meses";
			
			String plazoEnNumero = "";
			plazoEnNumero = dbPro.getPlazoDesembolso();
			
			Calendar fecha1 = new GregorianCalendar();
			String fechaApertura = "";
			SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy",Locale.ENGLISH);
			Date fecha = new Date(System.currentTimeMillis());
			//Date fecha = fecha1.getTime();
			fechaApertura = df.format(fecha);
			
			double tasa = 0;
			tasa = dbPro.getTasaInteres().doubleValue();
			
			String fechaVencimiento = "";
			
			
			String cantidad = plazoEnNumero;

			if(cantidad != null && cantidad.length() > 0){
				int cantInt = Integer.parseInt(cantidad) + 1; //sumarle 1 al mes, pues comienza en CERO
				Calendar cal = Calendar.getInstance();
				int mes = cal.getTime().getMonth(); 
				cantInt = cantInt + mes - 1;
				cal.set(Calendar.MONTH, +cantInt);
				String fechaCalculada = new SimpleDateFormat("dd/MM/yyyy").format(cal.getTime());
				System.out.println("fecha calculada: "+Calendar.MONTH);
				fechaVencimiento = fechaCalculada;
				
			}
	    	
			System.out.println("<<----------------------------------------->>");
			System.out.println("Mes : "+Calendar.MONTH);
			System.out.println("monto: " + linea.getMonto());  
			System.out.println("frecuenciaPago: " + frecuenciaPago);
			System.out.println("tipoPlazo: " + tipoPlazo);
			System.out.println("plazoEnNumero: " + plazoEnNumero);
			System.out.println("fechaApertura: " + fechaApertura + "");
			System.out.println("tasaRecalculada: " + tasa);
			System.out.println("fechaVencimiento: " + fechaVencimiento + "");
			System.out.println("<<----------------------------------------->>");
			
	    	if(tipoLineaCred.equalsIgnoreCase("A")){
	    		
	    		cuota = Servicios.calculoCuotaDesembolsoLC(getUsuario().getUsuarioAsString(), 
						"0", 
		    			dbPro.getClienteLinea(), 
		    			linea.getMonto(), 
						frecuenciaPago, 
						tipoPlazo, 
						plazoEnNumero, 
						fechaApertura, 
						tasa,  
						fechaVencimiento);
    		
	    		//cuota = new BigDecimal(100); // para pruebas
	
	    		System.out.println("APLICACION. DESEMBOLSO LC. Cuota de linea de credIto rotativa: " + cuota);
	    		System.out.println("APLICACION. DESEMBOLSO LC. fecha apertura de linea de credIto rotativa: " + fechaApertura);
	    		System.out.println("APLICACION. DESEMBOLSO LC. fecha vencimiento de credito rotativa: " + fechaVencimiento);
	    	}else {
	    			cuota = dbPro.getCuotaDesembolso();
	    			System.out.println("APLICACION. DESEMBOLSO LC. Cuota de linea de credIto fija: " + cuota);
	    	}
	    	
	    	if(frecuenciaPago.equals("MAT"))
	    		cuota = new BigDecimal(0);
	    	
	    	System.out.println("----> *numeroLinea*: " + dbPro.getNumeroLinea()+ "");
	    	System.out.println("----> *cuentaAbono*: " + dbPro.getCuentaAbono()+ "");
	    	
	    	Respuesta rp = Servicios.desembolsoLineasCredito(getUsuario().getUsuarioAsString(), 
	    			"0",
	    			dbPro.getClienteLinea(), 
	    			dbPro.getNumeroLinea(), 
	    			dbPro.getNumeroConvDesembLinea() !=null?dbPro.getNumeroConvDesembLinea().trim():"0",
	    			dbPro.getCuentaAbono(),//linea.getCuenta().getNumero(), 
	    			linea.getMonto(), 
	    			dbPro.getPlazoDesembolso(),//"6",//dbPro.getPlazoDesembolso(),
	    			cuota.doubleValue(),//16.67,//dbPro.getCuotaDesembolso().doubleValue(),
	    			dbPro.getTasaInteres().doubleValue(), 
	    			//dbPro.getReferenciaPrestamo(),
	    			dbPro.getReferenciaPrestamo() !=null?dbPro.getReferenciaPrestamo().trim():"0",
	    			dbPro.getTipoLinea());
	    	if(rp != null && rp.getListaContenedores().size() > 0){
 				rp.primerContenedor();
 				rp.siguiente();
 				String codigo = rp.obtenerString("codigo").trim();
 				String descripcion = rp.obtenerString("descripcion").trim();
 				System.out.println("RESPUESTA SERVICIO 6300100: "  + codigo+":"+descripcion);
 				
 				if(codigo.equalsIgnoreCase("000000")){
 					Respuesta rp1 = rp.obtenerSubRespuesta("cuerpo");
 					if(rp1 != null && rp1.getListaContenedores().size() > 0){
 						rp1.primerContenedor();
 						rp1.siguiente();
 						String prestamo = rp1.obtenerString("numPrestamo")!=null?rp1.obtenerString("numPrestamo").trim():"0";
 						result = MQResultado.APLICADO_MQ;
 						linea.setResultado(MQResultado.APLICADO_MQ);
 						linea.setMensajeDevolucion(0);
 						linea.setTramaEntradaAsString("Numero de prestamo:"+prestamo);
 						
 						//INICIO: controlar el campo de objeto xml de informacion transaccion desembolso en linea; OJO FALTA; CPALACIOS
 						//setear las propiedades siguientes pues dependen fecha aplicacion:
 						//fechaVencimiento, cuotaDesembolso
 						//aplica sopara el tipo de linea de credito ROTATIVA
 						if(tipoLineaCred.equalsIgnoreCase("A")){
 							dbPro.setCuotaDesembolso(cuota);
 							dbPro.setFechaPlazoDesembolso(fechaVencimiento);
 							dbPro.setReferenciaPrestamo(prestamo);
 							linea.setMemoAsBytes(dbPro.toBytes());
 						}
 	 		    	    //FIN: controlar el campo de objeto xml de informacion transaccion desembolso en linea
 	 		    		
 						HibernateMap.update(linea);
 					}
 				}else{
 					result = MQResultado.NO_PROCESADO;
 		    	    linea.setResultado(MQResultado.NO_PROCESADO);
 		    	    linea.setMensajeDevolucion(PropertyMaps.getCodigoErrorDesembolso(codigo).intValue());
 		    	    linea.setTramaEntradaAsString(codigo+":"+descripcion);
 		    	    HibernateMap.update(linea);
 				}
	    	}else{
	    		result = MQResultado.NO_PROCESADO;
	    	    linea.setResultado(MQResultado.NO_PROCESADO);
	    	    linea.setMensajeDevolucion(MQMensajeDevolucion.SERVICIO_TEMPORALMENTE_NO_DISPONIBLE);
	    	    linea.setTramaEntradaAsString("No hubo respuesta del servicio de desembolso en linea");   		
	    	    HibernateMap.update(linea);
	    	}	    	
	        Servicios.createAuditoria(getUsuario(), extra);		
    	}catch (Exception e){
		    e.printStackTrace();
		}		
		return result;		
	}

}
