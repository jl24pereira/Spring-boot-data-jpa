import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

import com.ba.potala.util.Constantes;
import com.ba.servicios.integracion.Servicios;
import com.fintec.comunes.FechaUtils;
import com.fintec.comunes.LogHelper;
import com.fintec.definiciones.Moneda;
import com.fintec.hibernate.HibernateMap;
import com.fintec.potala.bac.autorizaciones.AutorizacionesUtils;
import com.fintec.potala.corporativa.clazzes.Cliente;
import com.fintec.potala.corporativa.clazzes.EstadoTransaccion;
import com.fintec.potala.corporativa.clazzes.LineaTransaccion;
import com.fintec.potala.corporativa.clazzes.MQResultado;
import com.fintec.potala.corporativa.clazzes.TipoTransaccion;
import com.fintec.potala.corporativa.clazzes.Transaccion;
import com.fintec.potala.corporativa.clazzes.Usuario;
import com.fintec.potala.corporativa.services.TransaccionUtils;
import com.fintec.potala.bac.dq.trans.TransactionDispacher;
import com.fintec.potala.struts.clases.SessionKeys;
import com.fintec.potala.struts.clases.ParametrosSolicitud;
import com.fintec.potala.struts.actions.TransaccionGenericoAction;
import com.fintec.potala.struts.actions.ValidaSessionBaseAction;
import com.fintec.potala.struts.forms.TransaccionGenericoForm;
import com.fintec.potala.struts.forms.consultas.ConsultaComprobantesForm;
import com.fintec.potala.struts.forms.solicitudes.SolicitudAfiliacionTrxAutomaticasForm;
import com.fintec.potala.struts.forms.solicitudes.SolicitudGenericoForm;
import com.fintec.potala.web.bac.jdbc.sql.DetalleEgresoDivisasSQL;
import com.fintec.potala.web.clases.solicitudes.afiliaciontrxautom.SolicitudAfiliacionTrxAutomaticaPagina1;
import com.fintec.potala.web.clases.solicitudes.transferenciainternacional.Pagina2DatosSolicitante;
import com.fintec.potala.web.clases.solicitudes.transferenciainternacional.Pagina3DatosTransaccion;

/**
 * Este action es la base para todas las solicitudes del sistema:
 * 
 * Atributos 
 * session = objeto session del sistema 
 * usuario = se obtiene el objeto usuario de la session
 * parametros = objeto que contiene los parametros necesarios de un Action
 * 
 * 	Todas las solicitudes cumplen un mismo ciclo marcado por el atributo accion del form (SolicitudGenericoForm)
 *    Iniciar: Se limpia la session, se llama a l doInicio y se indica la actual igual a pagina1.
 * 	  Continuar: Se asigna el cliente de los parametros de la session a los parametros, se pasa a la pagina siguiente. 	  
 * 	  Volver: Se asigna el cliente de los parametros de la session a los parametros, se pasa a la pagina anterior.
 *    Aplicar: Se aplica o elimina la solicitud. Se envia el correo electronico de la solicitud. 
 * 			   Pasamos los datos de clave de transaccion editando a transaccion creada.
 *             Si el estado de la transaccion es ESTADO_ENVIADO_AS400 entonces no hay que guardarla porque de eso se encarga el despachador de transacciones.
 * 
 * Otros operaciones importantes: 
 * doInicio:   metodo que se invoca antes de pasar a la primera pagina, debe ser implementado en todas las clases que extiendan de el.
 * goNextPage: toma la pagina actual de la session dependiendo de la misma muestra la siguiente pagina
 * loadPageFromSession: carga las diferentes paginas de las solicitudes de la seccion.
 * loadPageFromRequest: carga las diferentes paginas de las solicitudes del request.
 * goPrevPage: toma la pagina actual de la session dependiendo de la misma muestra la pagina anterior
 * doBeforePageX: son metodos que se llaman antes de pasar de la pagina X-1 a la pagina X
 * doIt: Es el metodo que se llamara cuando pasemos de la ultima a la pagina de resultado.
 * mostrarErrorNoCuenta: Es el metodo que se llamara cuando exista un error en el action o en cualquiera que extienda de el. 
 * 
 * 
 * 
 * Las solicitudes tienen 1 forwards por cada pagina, 1 forward para resultados y uno para iniciar el caso de uso.
 * 
 * @author avega
 * @version 1.0
 */
abstract public class SolicitudGenericoAction extends ValidaSessionBaseAction {
 private static final int ACCESO_CORRECTO = 0;
 private static final int ACCESO_INCORRECTO = -1;
 private static final int TRANSACCION_YA_AUTORIZADA = -2;
 private static final int TRANSACCION_YA_APLICADA = -3;
 private static final int TRANSACCION_YA_PROCESADA = -4;
 private static final String SEMANAL = "S";
 private static final String QUINCENAL = "Q";
 private static final String MENSUAL = "M";
 private static final String TRIMESTRAL = "T";
 private static final int MAXIMO_MES = 12;

 /**
  * Procesa la peticion especificada del HTTP, y crea la respuesta correspondiente 
  * del HTTP (o remite a otro componente web que la cree).
  * Devuelve una instancia <code>ActionForward</code> describiendo como el control
  * debe ser remitido, o <code>null</code> si la respuesta se ha terminado ya.
  *
  * @param mapping El ActionMapping usado para seleccionar esta instancia
  * @param actionForm El opcional ActionForm bean para esta peticion (si existe uno)
  * @param request El HTTP request estamos procesando
  * @param response The HTTP response estamos creando
  *
  * @exception Excepcion si la logica del negocio del <code>Action</code> lanza una excepcion
  */
 public ActionForward executeAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
  HttpSession session = request.getSession(false);
  Usuario usuario = getUsuario(session);
  ParametrosSolicitud parametros = new ParametrosSolicitud();
  try {
   /*
    * Limpiamos los errores y los mensajes y seteamos los forms
    */
   parametros.setErrors(new ActionErrors());
   parametros.setMessages(new ActionMessages());
   parametros.setFormRequest((SolicitudGenericoForm) form);
   parametros.setFormSession((SolicitudGenericoForm) session.getAttribute(parametros.getFormRequest().getSessionKey()));
   parametros.setMapping(mapping);
   parametros.setCliente(usuario.getCliente());
   /*
    * Verificamos la accion
    * iniciar:		primera accion antes de cualquier solicitud
    * continuar:	segunda accion, es para pasar de una pagina a otra
    * volver:		accion que se encarga de pasar de la pagina actual a la anterior
    * aplicar:		se encarga de aplicar la solicitud, es la ultima accion para cumplir todo el ciclo de solicitud
    * recargar:		lo que hace es refrescar algunos datos de la pantalla
    */
   int acceso = this.estadoValido(parametros);
   System.out.println("acceso = " + acceso);
   if (acceso == ACCESO_CORRECTO) {
    if ("recargar".equalsIgnoreCase(parametros.getFormRequest().getAccion())) {
     parametros.setForward(this.executeRecargar(mapping, parametros.getFormRequest(), request, response, parametros));
     return parametros.getForward();
    }
    if ("seleccionar".equalsIgnoreCase(parametros.getFormRequest().getAccion())) {
     parametros.setForward(this.executeSelect(mapping, parametros.getFormRequest(), request, response, parametros));
     return parametros.getForward();
    }
    if ("consultarNombre".equalsIgnoreCase(parametros.getFormRequest().getAccion())) {
     parametros.setForward(this.executeConsultarNombre(mapping, parametros.getFormRequest(), request, response, parametros));
     return parametros.getForward();
    }
    if ("eliminar".equalsIgnoreCase(parametros.getFormRequest().getAccion())) {
     parametros.setForward(this.executeBorrar(mapping, parametros.getFormRequest(), request, response, parametros));
     return parametros.getForward();
    }
    if ("iniciar".equals(parametros.getFormRequest().getAccion())) {
     try {
      /** Validaciones necesarias para mostrar campo token **/
      if (TransaccionUtils.usuarioUnico(usuario) && TransaccionUtils.esTransaccionPorToken(parametros.getFormRequest().getTipoTransaccion())) {
       // Verificando si el usuario en efecto posee el token
       if (!usuario.getTieneToken())
        parametros.getErrors().add("transacciones", new ActionError("errors.transaccion.tokennecesario"));
      }
     } catch (Exception e) {
      System.out.println("Error validando si la transaccion es tokenizada");
      e.printStackTrace();
     }
     //Limpiamos la session con un form del mismo tipo del request
     parametros.setFormSession((SolicitudGenericoForm) parametros.getFormRequest().getClass().newInstance());
     //Llamamos al doInicio que debe ser implementado en todas las clases 
     parametros.getFormSession().getSolicitud().setCliente(parametros.getCliente());
     parametros.getFormRequest().getSolicitud().setCliente(parametros.getCliente());
     if (parametros.getFormRequest().getTipoTransaccion().equals(TipoTransaccion.SOLICITUD_TRANSFERENCIA_INTERNACIONAL)) {
      Cliente clienteParametroControl = parametros.getCliente();
      if (Servicios.esClienteListaControl(usuario.getUser(), clienteParametroControl)) {
       parametros.setForward(parametros.getMapping().findForward("pagina6"));
      } else {
       doInicio(parametros);
      }
     } else {
      doInicio(parametros);
     }
     //Indicamos que estamos en la pagina 1
     parametros.getFormSession().setPaginaActual(1);
     if (!parametros.getErrors().isEmpty()) {
      saveErrors(request, parametros.getErrors());
      parametros.setForward(mapping.findForward("error"));
     } else {
      parametros.setForward(parametros.getForward());
     }
    }
    if ("nuevo".equals(parametros.getFormRequest().getAccion())) {
     doInicio(parametros);
     System.out.println("getForward = " + parametros.getForward());
     //Indicamos que estamos en la pagina 1
     parametros.getFormSession().setPaginaActual(1);
    }
    //cargar los datos de la plantilla que se selecciono
    if ("seleccionarPlantilla".equals(parametros.getFormRequest().getAccion())) {
     doInicio(parametros);
     System.out.println("getForward = " + parametros.getForward());
     //Indicamos que estamos en la pagina 1
     parametros.getFormSession().setPaginaActual(1);
     this.doBeforePage1(request, parametros);
    }
    if ("continuar".equals(parametros.getFormRequest().getAccion())) {
     parametros.getFormSession().getSolicitud().setCliente(parametros.getCliente());
     parametros.getFormRequest().getSolicitud().setCliente(parametros.getCliente());
     goNextPage(request, parametros);
    }
    if ("volver".equals(parametros.getFormRequest().getAccion())) {
     parametros.getFormSession().getSolicitud().setCliente(parametros.getCliente());
     parametros.getFormRequest().getSolicitud().setCliente(parametros.getCliente());
     goPrevPage(parametros);
    }
    if ("guardar".equals(parametros.getFormRequest().getAccion())) {
     parametros.getFormSession().getSolicitud().setCliente(parametros.getCliente());
     parametros.getFormRequest().getSolicitud().setCliente(parametros.getCliente());
     ActionErrors errors = parametros.getErrors();
     //se suma la comision a la cantidad en caso de que la transaccion sea de tipo SOLICITUD_TRANSFERENCIA_INTERNACIONAL
     //valida los motivos de la transaccion si la cantidad es mayor 5000
     if (parametros.getFormRequest().getTipoTransaccion().equals(TipoTransaccion.SOLICITUD_TRANSFERENCIA_INTERNACIONAL)) {
      boolean fechaValida = validarFechaCalendarizada(parametros);
      parametros.getFormSession().setCalendarizacion(parametros.getFormRequest().getCalendarizacion());
      parametros.getFormSession().setCalendarizada(parametros.getFormRequest().getCalendarizada());
      parametros.getFormSession().setRecurrente(parametros.getFormRequest().getRecurrente());
      parametros.getFormSession().setPeriodo(parametros.getFormRequest().getPeriodo());
      parametros.getFormSession().setCantTrx(parametros.getFormRequest().getCantTrx());
      Pagina2DatosSolicitante p2 = (Pagina2DatosSolicitante) parametros.getFormRequest().getSolicitud().getPagina2();
      boolean tradeTicketExiste = Servicios.existeTradeTicket(p2.getSeleccionado(), "" + usuario.getCliente().getId());
      System.out.println("tradeTicket en linea de transaccion: " + tradeTicketExiste);
      if (tradeTicketExiste) {
       errors.add("solicitudTransferenciaInternacional", new ActionError("errors.transferencias.ticketenuso"));
      }
      if (errors.isEmpty() && fechaValida) {
       Pagina3DatosTransaccion pagina3 = (Pagina3DatosTransaccion) parametros.getFormRequest().getSolicitud().getPagina3();
       Moneda moneda = pagina3.getMoneda();
       boolean validarEgreso = false;
       boolean validarEgreso2 = false;
       double cantidad = pagina3.getCantidad().doubleValue();
       //Si es dolar se valida los motivos de la transaccion si la cantidad es mayor o igual 5000
       if (moneda.getId() == 1 && cantidad >= 5000) {
        validarEgreso = true;
       } else if (moneda.getId() != 1) {
        //Si la moneda no es dolar, el detalle de egreso de divisas es obligatorio
        validarEgreso = true;
       }
       System.out.println("MonedaId: " + moneda.getId());
       //Dennys Merino (Tecno-In) 20170208 validacion para el maximo de transacciones permitidas
       if (parametros.getFormRequest().getRecurrente()) {
        if (!(parametros.getFormRequest().getCantTrx() > 0 && parametros.getFormRequest().getCantTrx() <= 240)) {
         parametros.getErrors().add("solicitudTransferenciaInternacional", new ActionError("error.cant.Tranx.Recurrentes"));
        }
       }
       /*inicio: cpalacios; validacion para motivo a reportar FGR; febrero 2016*/
       //Si es dolar se valida los motivos de la transaccion si la cantidad es mayor o igual 5000
       if (moneda.getId() == 1 && cantidad >= 25000) {
        validarEgreso2 = true;
       } else if (moneda.getId() != 1) {
        //Si la moneda no es dolar, el detalle de egreso de divisas es obligatorio
        validarEgreso2 = true;
       }
       //Ahora el campo motivo de transaccion es obligatorio
       validarEgreso2 = true;
       System.out.println("validarEgreso2: " + validarEgreso2);
       if (validarEgreso2) {
        Pagina2DatosSolicitante pagina2 = (Pagina2DatosSolicitante) parametros.getFormRequest().getSolicitud().getPagina2();
        String motivoTransaccionReportar = pagina2.getMotivoTransaccionReportar();
        if (motivoTransaccionReportar != null) {
         if (motivoTransaccionReportar.trim().equals("")) {
          errors.add("solicitudTransferenciaInternacional", new ActionError("errors.transferencias.motivoTransaccion.norespuesta.reportar"));
         } else if (motivoTransaccionReportar.trim().length() < 4) {
          errors.add("solicitudTransferenciaInternacional", new ActionError("errors.transferencias.motivoTransaccion.norespuesta.minima"));
         }

        }

       } else {
        //cpalacios; validar si va este else o no
        Pagina2DatosSolicitante pagina2 = (Pagina2DatosSolicitante) parametros.getFormRequest().getSolicitud().getPagina2();
        String motivoTransaccionReportar = pagina2.getMotivoTransaccionReportar();
        if (motivoTransaccionReportar != null) {
         if (!motivoTransaccionReportar.trim().equals("") && motivoTransaccionReportar.trim().length() < 4) {
          errors.add("solicitudTransferenciaInternacional", new ActionError("errors.transferencias.motivoTransaccion.norespuesta.minima"));
         }

        }

       }
       /*fin: cpalacios; validacion para motivo a reportar FGR; febrero 2016*/

       //validacion obligatoria de el concepto de la transaccion y el pais
       if (true) {
        DetalleEgresoDivisasSQL detalleEgresoDivisasSQL = new DetalleEgresoDivisasSQL();
        Pagina2DatosSolicitante pagina2 = (Pagina2DatosSolicitante) parametros.getFormRequest().getSolicitud().getPagina2();
        String conceptoTransaccion1 = pagina2.getConceptoTransaccion1();
        String conceptoTransaccion2 = pagina2.getConceptoTransaccion2();
        String conceptoTransaccion3 = pagina2.getConceptoTransaccion3();
        String conceptoTransaccion4 = pagina2.getConceptoTransaccion4();
        String conceptoTransaccion5 = pagina2.getConceptoTransaccion5();
        String conceptoTransaccion6 = pagina2.getConceptoTransaccion6();

        if (conceptoTransaccion1 == null || "".equals(conceptoTransaccion1.trim())) {
         errors.add("solicitudTransferenciaInternacional", new ActionError("errors.transferencias.conceptoTransaccion.norespuesta.nivel1"));
        } else {
         List conceptoEgresosNivel2 = Servicios.getConceptos(usuario, 3, conceptoTransaccion1, "CDE");
         if (conceptoEgresosNivel2 != null && conceptoEgresosNivel2.size() > 0) {
          if (conceptoTransaccion2 == null || "".equals(conceptoTransaccion2.trim())) {
           errors.add("solicitudTransferenciaInternacional", new ActionError("errors.transferencias.conceptoTransaccion.norespuesta.nivel2"));
          } else {
           List conceptoEgresosNivel3 = Servicios.getConceptos(usuario, 4, conceptoTransaccion2, "CDE");
           if (conceptoEgresosNivel3 != null && conceptoEgresosNivel3.size() > 0) {
            if (conceptoTransaccion3 == null || "".equals(conceptoTransaccion3.trim())) {
             errors.add("solicitudTransferenciaInternacional", new ActionError("errors.transferencias.conceptoTransaccion.norespuesta.nivel3"));
            } else {
             List conceptoEgresosNivel4 = Servicios.getConceptos(usuario, 5, conceptoTransaccion3, "CDE");
             if (conceptoEgresosNivel4 != null && conceptoEgresosNivel4.size() > 0) {
              if (conceptoTransaccion4 == null || "".equals(conceptoTransaccion4.trim())) {
               errors.add("solicitudTransferenciaInternacional", new ActionError("errors.transferencias.conceptoTransaccion.norespuesta.nivel4"));
              } else {
               List conceptoEgresosNivel5 = Servicios.getConceptos(usuario, 6, conceptoTransaccion4, "CDE");
               if (conceptoEgresosNivel5 != null && conceptoEgresosNivel5.size() > 0) {
                if (conceptoTransaccion5 == null || "".equals(conceptoTransaccion5.trim())) {
                 errors.add("solicitudTransferenciaInternacional", new ActionError("errors.transferencias.conceptoTransaccion.norespuesta.nivel5"));
                } else {
                 List conceptoEgresosNivel6 = Servicios.getConceptos(usuario, 7, conceptoTransaccion5, "CDE");
                 if (conceptoEgresosNivel6 != null && conceptoEgresosNivel6.size() > 0) {
                  if (conceptoTransaccion6 == null || "".equals(conceptoTransaccion6.trim())) {
                   errors.add("solicitudTransferenciaInternacional", new ActionError("errors.transferencias.conceptoTransaccion.norespuesta.nivel6"));
                  }
                 }
                }
               }
              }
             }
            }
           }
          }
         }
        }

        if (pagina2.getPaisResidente() == null || "".equals(pagina2.getPaisResidente().trim())) {
         errors.add("solicitudTransferenciaInternacional", new ActionError("errors.transferencias.nopais"));
        }
       }
      }
     }
     if (errors.isEmpty()) {
      guardarSolicitud(parametros, request, usuario);
      copiarPaginasSession(parametros);
     } else {
      parametros.setForward(parametros.getMapping().findForward("pagina" + parametros.getFormRequest().getPaginaActual()));
     }

    }
    if ("aplicar".equals(parametros.getFormRequest().getAccion())) {
     parametros.getFormSession().getSolicitud().setCliente(parametros.getCliente());
     parametros.getFormRequest().getSolicitud().setCliente(parametros.getCliente());
     if (guardarSolicitud(parametros, request, usuario)) {
      int result = TransactionDispacher.execute(parametros.getFormSession().getTransaccion(), usuario);
      //Cargar el mensaje de respuesta del AS
      {
       LineaTransaccion linea;
       Iterator iter = parametros.getFormSession().getTransaccion().getLineaTransaccionList().iterator();
       while (iter.hasNext()) {
        linea = (LineaTransaccion) iter.next();
        // Si fue procesada entonces guardamos el numero de referencia
        if (linea.getResultado() == 0) {
         if (!parametros.getFormSession().getTransaccion().getTipoTransaccion().equals(TipoTransaccion.SOLICITUD_AFILIACION_TRX_AUTOMATICA)) {
          parametros.getFormSession().setReferenciaOperacion(linea.getNumeroComprobante());
          parametros.getMessages().add("solicitudes", new ActionMessage("mensaje.simple", "Transacci&oacute;n aplicada"));
         } else {
          if (parametros.getFormSession() instanceof SolicitudAfiliacionTrxAutomaticasForm) {
           SolicitudAfiliacionTrxAutomaticasForm form1 = (SolicitudAfiliacionTrxAutomaticasForm) parametros.getFormSession();
           SolicitudAfiliacionTrxAutomaticaPagina1 pagina1 = (SolicitudAfiliacionTrxAutomaticaPagina1) form1.getSolicitud().getPagina1();
           String validaReferencia = pagina1.getConvenioServicio().getValidacionReferencia();
           if (validaReferencia.equals("N")) {
            parametros.getMessages().add("solicitudes", new ActionMessage("mensaje.simple", "Solicitud Aprobada"));
           } else {
            parametros.getMessages().add("solicitudes", new ActionMessage("mensaje.simple", "Solicitud en Tr&aacute;mite, pendiente de activaci&oacute;n"));
           }
          }
         }
        }
        // Si proceso la linea y no la aplico entonces enviamos el mensaje de rechazo 
        if (linea.getResultado() != 0 && linea.getResultado() != MQResultado.NO_PROCESADO) {
         System.out.println("Devolucion>>>" + linea.getMensajeDevolucionAsString());
         parametros.getMessages().add("solicitudes", new ActionMessage("mensaje.simple", linea.getMensajeDevolucionAsString()));
        }
        if (linea.getResultado() != 0 && linea.getResultado() == MQResultado.NO_PROCESADO) {
         System.out.println("MSG >>>" + linea.getMensajeDevolucionAsString());
         parametros.getErrors().add("solicitudes", new ActionError("mensaje.simple", linea.getMensajeDevolucionAsString()));
        }
       }
      }
      copiarPaginasSession(parametros);
      System.out.println("GuardaComprobante>" + parametros.getFormSession().getTransaccion().getId());
      ConsultaComprobantesForm comprobanteForm = new ConsultaComprobantesForm();
      comprobanteForm.setTransaccion(parametros.getFormSession().getTransaccion());
      session.setAttribute(SessionKeys.KEY_COMPROBANTES, comprobanteForm);
     }
    }
   } else {
    if (acceso == TRANSACCION_YA_AUTORIZADA) {
     System.out.println(" La transaccion ya fue enviada");
     //----Transaccion ya autorizada
     parametros.getErrors().add("transacciones", new ActionError("errors.transaccion.porautorizar"));
    } else {
     if (acceso == TRANSACCION_YA_APLICADA) {
      System.out.println(" La transaccion ya fue aplicada");
      //----Transaccion ya aplicada
      parametros.getErrors().add("transacciones", new ActionError("errors.transaccion.aplicada"));
     } else {
      if (acceso == TRANSACCION_YA_PROCESADA) {
       System.out.println(" La transaccion ya fue procesada");
       //----Transaccion ya aplicada
       parametros.getErrors().add("transacciones", new ActionError("errors.transaccion.procesada"));
      } else {
       System.out.println(" El estado de la transaccion es invalido");
       parametros.getErrors().add("transacciones", new ActionError("errors.general.invalidaccess"));
      }
     }
    }
    parametros.setForward(mapping.findForward("error"));
   }
   /* Salvamos los errores, los mensajes, forms etc    */
   saveErrors(request, parametros.getErrors());
   saveMessages(request, parametros.getMessages());
   session.setAttribute(parametros.getFormRequest().getSessionKey(), parametros.getFormSession());
   request.setAttribute(parametros.getFormRequest().getSessionKey(), parametros.getFormRequest());
  } catch (Exception e) {
   parametros.setForward(mostrarErrorInesperado(e, mapping, parametros.getErrors()));
   saveErrors(request, parametros.getErrors());
  }
  return parametros.getForward();
 }

 private boolean guardarSolicitud(ParametrosSolicitud parametros, HttpServletRequest request, Usuario usuario) throws Exception {
  boolean result = false;
  System.out.println("voy a guardar la solicitud");
  HttpSession session = request.getSession(false);
  parametros.getFormSession().getSolicitud().setCliente(parametros.getCliente());
  parametros.getFormRequest().getSolicitud().setCliente(parametros.getCliente());
  if (validarForm(parametros.getFormRequest().getPaginaActual(), request, parametros)) {
   if (this.doIt(session, parametros) == 0) {
    //----Pasamos los datos de la session al request
    PropertyUtils.copyProperties(parametros.getFormRequest(), parametros.getFormSession());
    if (parametros.getFormRequest().getCalendarizada()) {
     System.out.println("Seteamos la fecha de calendarizacion");
     parametros.getFormSession().getTransaccion().setFechaProgramacionAutomatica(parametros.getFormRequest().getCalendarizacion().getFechaAsDate().getTime());
     System.out.println("Calendarizada");
    }
    LineaTransaccion linea = (LineaTransaccion) parametros.getFormSession().getTransaccion().getLineaTransaccionList().get(0);
    if (linea.getNumeroComprobante() == null) {
     linea.setResultado(MQResultado.NO_PROCESADO);
     linea.setNumeroComprobante("");
    }
    //cpalacios; auditoria usuarios; aplica?; 20160624; ip
    if (parametros.getFormRequest().getTipoTransaccion().equals(TipoTransaccion.SOLICITUD_AFILIACION_TRX_AUTOMATICA)) {
     String ip = "";
     ip = request.getRemoteAddr() != null ? request.getRemoteAddr() : " ";
     linea.setNumeroComprobante(ip);
    }
    //----Completamos los datos de la transaccion
    parametros.getFormSession().getTransaccion().setUsuario(usuario);
    parametros.getFormSession().getTransaccion().setCliente(usuario.getCliente());
    parametros.getFormSession().getTransaccion().setFechaUltimaModificacion(new GregorianCalendar().getTime().getTime());
    // saving
    {
     List logs = new ArrayList();
     try {
      TransaccionUtils.saveAndWait(logs, parametros.getFormSession().getTransaccion());
      parametros.getFormRequest().setEstado(TransaccionGenericoForm.ESTADO_EJECUTADO);
      parametros.getFormSession().setEstado(TransaccionGenericoForm.ESTADO_EJECUTADO);
      //Verificamos si se ha programado para que sea recurrente...
      if (parametros.getFormSession().getCalendarizada() && parametros.getFormSession().getRecurrente() && parametros.getFormSession().getCantTrx() > 0) {
       crearTransaccionesProgramadas(logs, parametros.getFormSession(), parametros.getFormSession().getTransaccion(), true);
      }
     } finally {
      LogHelper.list(logs);
     }
    }
    //----Actualizamos las fechas
    parametros.getFormRequest().setFechaSistema(parametros.getFormRequest().getFechaFromIBS());
    result = true;
    System.out.println("solicitud guardada");
   }
  }
  return result;
 }

 private void copiarPaginasSession(ParametrosSolicitud parametros) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
  //----Cargamos los datos de las paginas de la session a las paginas del request
  if (parametros.getFormSession().getSolicitud().getPagina1() != null) {
   PropertyUtils.copyProperties(parametros.getFormRequest().getSolicitud().getPagina1(), parametros.getFormSession().getSolicitud().getPagina1());
  }
  if (parametros.getFormSession().getSolicitud().getPagina2() != null) {
   PropertyUtils.copyProperties(parametros.getFormRequest().getSolicitud().getPagina2(), parametros.getFormSession().getSolicitud().getPagina2());
  }
  if (parametros.getFormSession().getSolicitud().getPagina3() != null) {
   PropertyUtils.copyProperties(parametros.getFormRequest().getSolicitud().getPagina3(), parametros.getFormSession().getSolicitud().getPagina3());
  }
  if (parametros.getFormSession().getSolicitud().getPagina4() != null) {
   PropertyUtils.copyProperties(parametros.getFormRequest().getSolicitud().getPagina4(), parametros.getFormSession().getSolicitud().getPagina4());
  }
  if (parametros.getFormSession().getSolicitud().getPagina5() != null) {
   PropertyUtils.copyProperties(parametros.getFormRequest().getSolicitud().getPagina5(), parametros.getFormSession().getSolicitud().getPagina5());
  }
  if (parametros.getFormSession().getSolicitud().getPagina6() != null) {
   PropertyUtils.copyProperties(parametros.getFormRequest().getSolicitud().getPagina6(), parametros.getFormSession().getSolicitud().getPagina6());
  }
 }

 /**
  * doInicio: es el metodo que se invoca antes de pasar a la primera pagina,
  * este metodo debe ser implementado en todas las clases que extiendan de el.
  */
 abstract public void doInicio(ParametrosSolicitud parametros) throws Exception;

 /**
  * 
  * @param pagina
  * @param request
  * @param parametros
  * @return
  * @throws Exception
  */
 private boolean validarForm(int pagina, HttpServletRequest request, ParametrosSolicitud parametros) throws Exception {
  parametros.getFormRequest().setPage(pagina);
  for (int i = 0; i < pagina; i++) {
   loadPageFromSession(i, parametros);
  }
  ActionErrors errors = parametros.getFormRequest().validate(parametros.getMapping(), request);
  if (!errors.isEmpty()) {
   parametros.setErrors(errors);
   parametros.setForward(parametros.getMapping().findForward("pagina" + pagina));
   return false;
  }
  return true;
 }

 /**
  * goNextPage: toma la pagina actual de la session dependiendo de la misma muestra la siguiente pagina
  */
 private void goNextPage(HttpServletRequest request, ParametrosSolicitud parametros) throws Exception {
  //----Tomamos la pagina actual (que esta en la session)
  int pagina = parametros.getFormRequest().getPaginaActual();
  boolean pasarPagina = false;
  System.out.println("pagina actual = " + pagina);
  if (!validarForm(pagina, request, parametros)) {
   return;
  }

  //----Dependiendo de la pagina donde queramos ir entonces validamos la pagina y seguimos
  if (pagina == 1) {
   System.out.println("Voy para la pagina 2");
   pasarPagina = this.doBeforePage2(request, parametros);
  }
  if (pagina == 2) {
   System.out.println("Voy para la pagina 3");
   pasarPagina = this.doBeforePage3(request, parametros);
  }
  if (pagina == 3) {
   System.out.println("Voy para la pagina 4");
   pasarPagina = this.doBeforePage4(request, parametros);
  }
  if (pagina == 4) {
   System.out.println("Voy para la pagina 5");
   pasarPagina = this.doBeforePage5(request, parametros);
  }
  if (pagina == 5) {
   System.out.println("Voy para la pagina 6");
   pasarPagina = this.doBeforePage6(request, parametros);
  }

  //----Si podemos pasar de pagina entonces actualizamos la pagina de la session entonces seteamos el forward
  if (pasarPagina) {
   System.out.println("Podemos pasar al forward pagina" + (pagina + 1) + "");
   parametros.getFormSession().setPaginaActual(pagina + 1);
   parametros.setForward(parametros.getMapping().findForward("pagina" + (pagina + 1)));
   if (!parametros.getFormRequest().getTipoTransaccion().equals(TipoTransaccion.SOLICITUD_TRANSFERENCIA_INTERNACIONAL) &&
    !parametros.getFormRequest().getTipoTransaccion().equals(TipoTransaccion.SOLICITUD_AFILIACION_TRX_AUTOMATICA)) {
    loadPageFromSession(pagina + 1, parametros);
   }
   loadPageFromRequest(pagina, parametros);
  } else {
   if (!parametros.getErrors().isEmpty()) {
    System.out.println("Tenemos que quedarnos en la misma pagina para mostrar mensaje");
    //----Si no podemos pasar de pagina y no hay errores entonces nos quedamos en la pagina actual
    parametros.setForward(parametros.getMapping().findForward("pagina" + pagina));
   }
  }
 }

 /**
  * loadPageFromSession: carga las diferentes paginas de las solicitudes de la seccion.
  * Tenemos 6 paginas debido a que la solicitud mas grande tiene 6 paginas.
  **/
 private void loadPageFromSession(int pagina, ParametrosSolicitud parametros) throws Exception {
  if (pagina == 1) {
   PropertyUtils.copyProperties(parametros.getFormRequest().getSolicitud().getPagina1(), parametros.getFormSession().getSolicitud().getPagina1());
  }
  if (pagina == 2) {
   PropertyUtils.copyProperties(parametros.getFormRequest().getSolicitud().getPagina2(), parametros.getFormSession().getSolicitud().getPagina2());
  }
  if (pagina == 3) {
   PropertyUtils.copyProperties(parametros.getFormRequest().getSolicitud().getPagina3(), parametros.getFormSession().getSolicitud().getPagina3());
  }
  if (pagina == 4) {
   PropertyUtils.copyProperties(parametros.getFormRequest().getSolicitud().getPagina4(), parametros.getFormSession().getSolicitud().getPagina4());
  }
  if (pagina == 5) {
   PropertyUtils.copyProperties(parametros.getFormRequest().getSolicitud().getPagina5(), parametros.getFormSession().getSolicitud().getPagina5());
  }
  if (pagina == 6) {
   PropertyUtils.copyProperties(parametros.getFormRequest().getSolicitud().getPagina6(), parametros.getFormSession().getSolicitud().getPagina6());
  }
 }

 /**
  * loadPageFromRequest: carga las diferentes paginas de las solicitudes del request.
  * Tenemos 6 paginas debido a que la solicitud mas grande tiene 6 paginas.
  **/
 private void loadPageFromRequest(int pagina, ParametrosSolicitud parametros) throws Exception {
  if (pagina == 1) {
   PropertyUtils.copyProperties(parametros.getFormSession().getSolicitud().getPagina1(), parametros.getFormRequest().getSolicitud().getPagina1());
  }
  if (pagina == 2) {
   PropertyUtils.copyProperties(parametros.getFormSession().getSolicitud().getPagina2(), parametros.getFormRequest().getSolicitud().getPagina2());
  }
  if (pagina == 3) {
   PropertyUtils.copyProperties(parametros.getFormSession().getSolicitud().getPagina3(), parametros.getFormRequest().getSolicitud().getPagina3());
  }
  if (pagina == 4) {
   PropertyUtils.copyProperties(parametros.getFormSession().getSolicitud().getPagina4(), parametros.getFormRequest().getSolicitud().getPagina4());
  }
  if (pagina == 5) {
   PropertyUtils.copyProperties(parametros.getFormSession().getSolicitud().getPagina5(), parametros.getFormRequest().getSolicitud().getPagina5());
  }
  if (pagina == 6) {
   PropertyUtils.copyProperties(parametros.getFormSession().getSolicitud().getPagina6(), parametros.getFormRequest().getSolicitud().getPagina6());
  }
 }

 /**
  * goPrevPage: toma la pagina actual de la session dependiendo de la misma muestra la pagina anterior
  */
 private void goPrevPage(ParametrosSolicitud parametros) throws Exception {
  //----Tomamos la pagina actual (que esta en la session)
  int pagina = parametros.getFormRequest().getPaginaActual() - 1;

  //----Dependiendo de la pagina donde queramos ir entonces pasamos los datos del objeto en session al request
  try {
   System.out.println("devolviendome a la pagina " + pagina);

   //----Cargamos los datos de las paginas de la session a las paginas del request
   if (parametros.getFormSession().getSolicitud().getPagina1() != null) {
    PropertyUtils.copyProperties(parametros.getFormRequest().getSolicitud().getPagina1(), parametros.getFormSession().getSolicitud().getPagina1());
   }
   if (parametros.getFormSession().getSolicitud().getPagina2() != null) {
    PropertyUtils.copyProperties(parametros.getFormRequest().getSolicitud().getPagina2(), parametros.getFormSession().getSolicitud().getPagina2());
   }
   if (parametros.getFormSession().getSolicitud().getPagina3() != null) {
    PropertyUtils.copyProperties(parametros.getFormRequest().getSolicitud().getPagina3(), parametros.getFormSession().getSolicitud().getPagina3());
   }
   if (parametros.getFormSession().getSolicitud().getPagina4() != null) {
    PropertyUtils.copyProperties(parametros.getFormRequest().getSolicitud().getPagina4(), parametros.getFormSession().getSolicitud().getPagina4());
   }
   if (parametros.getFormSession().getSolicitud().getPagina5() != null) {
    PropertyUtils.copyProperties(parametros.getFormRequest().getSolicitud().getPagina5(), parametros.getFormSession().getSolicitud().getPagina5());
   }
   if (parametros.getFormSession().getSolicitud().getPagina6() != null) {
    PropertyUtils.copyProperties(parametros.getFormRequest().getSolicitud().getPagina6(), parametros.getFormSession().getSolicitud().getPagina6());
   }
  } catch (Exception e) {
   //----En este caso cualquier error entonces pasamos a la pagina anterior sin setear los datos de la pagina
   System.out.println("Error en goPrevPage en  " + this.getClass().getName());
   e.printStackTrace();
  }

  parametros.setForward(parametros.getMapping().findForward("pagina" + pagina));
 }

 /**
  * doBeforePageX: son metodos que se llaman antes de pasar de la pagina X-1 a la pagina X
  * @return 	true,  si podemos pasar de la pagina X-1 a la pagina X
  * 			false, si no podemos pasar de la pagina X-1 a la pagina X
  */
 public boolean doBeforePage1(HttpServletRequest request, ParametrosSolicitud parametros) throws Exception {
  return true;
 }

 public boolean doBeforePage2(HttpServletRequest request, ParametrosSolicitud parametros) throws Exception {
  return true;
 }

 public boolean doBeforePage3(HttpServletRequest request, ParametrosSolicitud parametros) throws Exception {
  return true;
 }

 public boolean doBeforePage4(HttpServletRequest request, ParametrosSolicitud parametros) throws Exception {
  return true;
 }

 public boolean doBeforePage5(HttpServletRequest request, ParametrosSolicitud parametros) throws Exception {
  return true;
 }

 public boolean doBeforePage6(HttpServletRequest request, ParametrosSolicitud parametros) throws Exception {
  return true;
 }

 /**
  * doIt: Es el metodo que se llamara cuando pasemos de la ultima a la pagina de resultado, al igual que doInicio
  * este metodo debe ser implementado en todas las clases que extiendan a el.
  */
 public int doIt(HttpSession session, ParametrosSolicitud parametros) throws Exception {
  parametros.getFormSession().getTransaccion().setEstado(EstadoTransaccion.ESTADO_PENDIENTE_APLICACION);
  return 0;
 }

 /**
  * mostrarErrorNoCuenta: Es el metodo que se llamara cuando exista un error en el action o en cualquiera que 
  * extienda de el, se llama el forward global (error) que envia a la pagina Error.jsp.
  */
 public final void mostrarErrorNoCuenta(ParametrosSolicitud parametros) {
  parametros.getErrors().add("solicitudtransferencias", new ActionError("errors.saldos.nocuentas"));
  parametros.setForward(parametros.getMapping().findForward("error"));
 }

 /**
  * Evalua el estado actual de la transaccion para asegurar una secuencia de pasos correcta
  * creacion -> confirmacion -> aplicacion
  * creacion -> modificacion -> confirmacion -> aplicacion (transacciones multiples como planillas)
  * @param parametros Recibe los datos generales de la transaccion, asi como los datos del request, la session, y el
  * objeto de mensajes, errores y forwards
  * @return -1 : Acceso incorrecto. Cuando no hay coordinacion entre los pasos ejecutados y el paso que se desea ejecutar.
  * Por ejemplo que el usuario desea realizar el paso de aplicacion si haber ejecutado el paso de confirmacion.
  * 0  : Acceso correcto. Cuando toda la coordinacion y el estado de la transaccion concuerdan
  * -2 : Transaccion ya autorizada. Cuando la transaccion ya esta aplicada o autorizada, esta condicion impide al usuario modificar la transaccion.
  */

 private int estadoValido(ParametrosSolicitud parametros) {
  int estado = ACCESO_CORRECTO; //----Acceso valido
  //----Si el estado del form del request es inicial entonces es valido
  if (parametros.getFormRequest().getEstado().equals(TransaccionGenericoForm.ESTADO_INICIAL) && parametros.getFormRequest().getAccion().equals(TransaccionGenericoForm.ESTADO_INICIAL)) {
   return ACCESO_CORRECTO; //----Acceso valido
  }

  //----Si ya se ejecuto entonces es invalido
  if (parametros.getFormSession() != null && parametros.getFormSession().getEstado().equals(TransaccionGenericoForm.ESTADO_EJECUTADO)) {
   try {
    if (parametros.getFormSession().getTransaccion().getEstado().equals(EstadoTransaccion.ESTADO_APLICADA)) {
     estado = TRANSACCION_YA_APLICADA; //----Transaccion ya aplicada
    } else if (parametros.getFormSession().getTransaccion().getEstado().equals(EstadoTransaccion.ESTADO_PENDIENTE_AUTORIZACION)) {
     estado = TRANSACCION_YA_AUTORIZADA; //----Transaccion ya autorizada
    } else {
     estado = TRANSACCION_YA_PROCESADA; //----Transaccion ya autorizada
    }
   } catch (Exception e) {
    // TODO: la transaccion puede estar null...revisar si este caso sucede
    System.out.println(LogHelper.log(e));
    return TRANSACCION_YA_PROCESADA;
    //----Transaccion ya autorizada
   }
  }

  return estado; //----Acceso valido
 }

 /**
  * Metodo incluido para realizar la recarga de datos como catalogos
  * dependiendo de la seleccion de alguna opcion 
  * @param mapping
  * @param form
  * @param request
  * @param response
  * @return
  * @throws Exception
  */
 public ActionForward executeRecargar(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response, ParametrosSolicitud parametros) throws Exception {
  throw new Exception("Falta la implementacion de executeRecargar en " + this.getClass().getName());
 }
 public ActionForward executeSelect(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response, ParametrosSolicitud parametros) throws Exception {
  throw new Exception("Falta la implementacion de executeRecargar en " + this.getClass().getName());
 }
 public ActionForward executeConsultarNombre(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response, ParametrosSolicitud parametros) throws Exception {
  throw new Exception("Falta la implementacion de executeConsultarNombre en " + this.getClass().getName());
 }

 public ActionForward executeBorrar(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response, ParametrosSolicitud parametros) throws Exception {
  throw new Exception("Falta la implementacion de executeBorrar en " + this.getClass().getName());
 }

 private boolean validarFechaCalendarizada(ParametrosSolicitud parametros) {
  //----Ahora validamos la fecha y la hora siempre y cuando sea una transaccion calendarizada
  boolean fechaValida = true;
  if (parametros.getFormRequest().getCalendarizada()) {
   try {
    GregorianCalendar fecha = parametros.getFormRequest().getCalendarizacion().getFechaAsCalendar();
    if (fecha.getTime().before(new GregorianCalendar().getTime())) {
     parametros.getErrors().add("calendarizacion", new ActionMessage("errors.calendarizacion.fechaanterior"));
     fechaValida = false;
    }
   } catch (Exception e) {
    parametros.getErrors().add("calendarizacion", new ActionMessage("errors.calendarizacion.fechainvalida"));
    fechaValida = false;
   }
  }

  return fechaValida;
 }

 public void crearTransaccionesProgramadas(List logs, TransaccionGenericoForm transaccionGenericoFrom, Transaccion transaccionSession, boolean wait) throws Exception {
  if (transaccionGenericoFrom.getPeriodo() != null) {
   //Semanal
   if (transaccionGenericoFrom.getPeriodo().trim().equalsIgnoreCase(SEMANAL)) {
    createTransaccionSemanales(logs, transaccionGenericoFrom, transaccionSession, wait);
   }
   //Quincenal
   if (transaccionGenericoFrom.getPeriodo().trim().equalsIgnoreCase(QUINCENAL)) {
    createTransaccionQuincenales(logs, transaccionGenericoFrom, transaccionSession, wait);
   }
   //Mensual
   if (transaccionGenericoFrom.getPeriodo().trim().equalsIgnoreCase(MENSUAL)) {
    createTransaccionMensuales(logs, transaccionGenericoFrom, transaccionSession, wait);
   }
   //Trimestral
   if (transaccionGenericoFrom.getPeriodo().trim().equalsIgnoreCase(TRIMESTRAL)) {
    createTransaccionTrimestrales(logs, transaccionGenericoFrom, transaccionSession, wait);
   }
  }
 }

 /**
  * Permite crear transacciones programadas SEMANALMENTE de forma recurrente,
  * segun la fecha de inicio y la cantidad de transacciones
  * especificadas, para la transaccion que se esta procesando en ese
  * momento 
  * @param logs
  * @param transaccionGenericoFrom
  * @param transaccionSession
  * @param wait
  * @throws Exception
  */
 public void createTransaccionSemanales(List logs, TransaccionGenericoForm transaccionGenericoFrom, Transaccion transaccionSession, boolean wait) throws Exception {
  //Obtengo la fecha de pago registrada
  int diaPago = transaccionGenericoFrom.getCalendarizacion().getFechaAplicacionDia();
  int mesPago = transaccionGenericoFrom.getCalendarizacion().getFechaAplicacionMes();
  int anioPago = transaccionGenericoFrom.getCalendarizacion().getFechaAplicacionAnno();
  int hora = transaccionGenericoFrom.getCalendarizacion().getFechaAplicacionHora();

  //Obtenemos datos necesarios
  int daysInAMonth = FechaUtils.getNumberDaysInAMonth(anioPago, mesPago);
  int dayOfWeekForADate = FechaUtils.getDayOfWeekForAPaticularDate(anioPago, mesPago, diaPago);
  int newDayOfWeekForOtherDate = 0;
  //Creamos la cantidad de transacciones especificadas
  for (int i = 0; i < (transaccionGenericoFrom.getCantTrx() - 1); i++) {
   diaPago += 7;
   if (diaPago > daysInAMonth) {
    diaPago -= daysInAMonth;
    mesPago += 1;

    if (mesPago > MAXIMO_MES) {
     switch (mesPago) {
      case 13:
       mesPago = 1;
       break;
     }
     anioPago += 1;
    }
    //Obtenemos la cantidad de dias del mes para tener un parametro mas exacto
    daysInAMonth = FechaUtils.getNumberDaysInAMonth(anioPago, mesPago);
   }

   newDayOfWeekForOtherDate = FechaUtils.getDayOfWeekForAPaticularDate(anioPago, mesPago, diaPago);
   if (dayOfWeekForADate == newDayOfWeekForOtherDate) {
    // actualizamos la fecha de la programacion
    transaccionGenericoFrom.getCalendarizacion().setFechaAplicacionDia(diaPago);
    transaccionGenericoFrom.getCalendarizacion().setFechaAplicacionMes(mesPago);
    transaccionGenericoFrom.getCalendarizacion().setFechaAplicacionAnno(anioPago);
    transaccionGenericoFrom.getCalendarizacion().setFechaAplicacionHora(hora);

    transaccionSession.setFechaProgramacionAutomatica(transaccionGenericoFrom.getCalendarizacion().getFechaAsDate().getTime());
    transaccionSession.setEstado(EstadoTransaccion.ESTADO_PROGRAMADA_POR_APLICAR);
    TransaccionUtils.save(logs, transaccionSession, wait);
   } else {
    System.err.println("**:o( Error para la fecha!!!==>" + diaPago + "/" + mesPago + "/" + anioPago);
   }
  }
 }

 /**
  * Permite crear transacciones programadas QUINCENALMENTE de forma recurrente,
  * segun la fecha de inicio y la cantidad de transacciones
  * especificadas, para la transaccion que se esta procesando en ese
  * momento 
  * @param logs
  * @param transaccionGenericoFrom
  * @param transaccionSession
  * @param wait
  * @throws Exception
  */
 public void createTransaccionQuincenales(List logs, TransaccionGenericoForm transaccionGenericoFrom, Transaccion transaccionSession, boolean wait) throws Exception {
  int diaPago = transaccionGenericoFrom.getCalendarizacion().getFechaAplicacionDia();
  int mesPago = transaccionGenericoFrom.getCalendarizacion().getFechaAplicacionMes();
  int anioPago = transaccionGenericoFrom.getCalendarizacion().getFechaAplicacionAnno();
  int hora = transaccionGenericoFrom.getCalendarizacion().getFechaAplicacionHora();

  //Obtenemos datos necesarios
  int daysInAMonth = FechaUtils.getNumberDaysInAMonth(anioPago, mesPago);

  //Creamos la cantidad de transacciones especificadas
  for (int i = 0; i < (transaccionGenericoFrom.getCantTrx() - 1); i++) {
   diaPago += 15;

   if (diaPago > daysInAMonth) {
    diaPago -= daysInAMonth;
    mesPago += 1;

    if (mesPago > MAXIMO_MES) {
     switch (mesPago) {
      case 13:
       mesPago = 1;
       break;
     }
     anioPago += 1;
    }
    //Obtenemos la cantidad de dias del mes para tener un parametro mas exacto
    daysInAMonth = FechaUtils.getNumberDaysInAMonth(anioPago, mesPago);
   }

   // actualizamos la fecha de la programacion
   transaccionGenericoFrom.getCalendarizacion().setFechaAplicacionDia(diaPago);
   transaccionGenericoFrom.getCalendarizacion().setFechaAplicacionMes(mesPago);
   transaccionGenericoFrom.getCalendarizacion().setFechaAplicacionAnno(anioPago);
   transaccionGenericoFrom.getCalendarizacion().setFechaAplicacionHora(hora);

   transaccionSession.setFechaProgramacionAutomatica(transaccionGenericoFrom.getCalendarizacion().getFechaAsDate().getTime());
   transaccionSession.setEstado(EstadoTransaccion.ESTADO_PROGRAMADA_POR_APLICAR);
   TransaccionUtils.save(logs, transaccionSession, wait);
  }
 }

 /**
  * Permite crear transacciones programadas MENSUALMENTE de forma recurrente,
  * segun la fecha de inicio y la cantidad de transacciones
  * especificadas, para la transaccion que se esta procesando en ese
  * momento 
  * @param logs
  * @param transaccionGenericoFrom
  * @param transaccionSession
  * @param wait
  * @throws Exception
  */
 public void createTransaccionMensuales(List logs, TransaccionGenericoForm transaccionGenericoFrom, Transaccion transaccionSession, boolean wait) throws Exception {
  int diaPago = transaccionGenericoFrom.getCalendarizacion().getFechaAplicacionDia();
  int mesPago = transaccionGenericoFrom.getCalendarizacion().getFechaAplicacionMes();
  int anioPago = transaccionGenericoFrom.getCalendarizacion().getFechaAplicacionAnno();
  int hora = transaccionGenericoFrom.getCalendarizacion().getFechaAplicacionHora();

  //Creamos la cantidad de transacciones especificadas
  int daysInAMonth = FechaUtils.getNumberDaysInAMonth(anioPago, mesPago);
  int diaPagoReal = 0;
  for (int i = 0; i < (transaccionGenericoFrom.getCantTrx() - 1); i++) {
   diaPagoReal = diaPago;
   mesPago += 1;

   if (mesPago > 12) {
    switch (mesPago) {
     case 13:
      mesPago = 1;
      break;
    }
    anioPago += 1;
   }
   //Obtenemos la cantidad de dias del mes para tener un parametro mas exacto
   daysInAMonth = FechaUtils.getNumberDaysInAMonth(anioPago, mesPago);

   if (diaPagoReal > daysInAMonth) {
    diaPagoReal = daysInAMonth;
   }

   // actualizamos la fecha de la programacion
   transaccionGenericoFrom.getCalendarizacion().setFechaAplicacionDia(diaPagoReal);
   transaccionGenericoFrom.getCalendarizacion().setFechaAplicacionMes(mesPago);
   transaccionGenericoFrom.getCalendarizacion().setFechaAplicacionAnno(anioPago);
   transaccionGenericoFrom.getCalendarizacion().setFechaAplicacionHora(hora);

   transaccionSession.setFechaProgramacionAutomatica(transaccionGenericoFrom.getCalendarizacion().getFechaAsDate().getTime());
   transaccionSession.setEstado(EstadoTransaccion.ESTADO_PROGRAMADA_POR_APLICAR);
   TransaccionUtils.save(logs, transaccionSession, wait);
  }
 }

 /**
  * 
  * @param logs
  * @param transaccionGenericoFrom
  * @param transaccionSession
  * @param wait
  * @throws Exception
  */
 public void createTransaccionTrimestrales(List logs, TransaccionGenericoForm transaccionGenericoFrom, Transaccion transaccionSession, boolean wait) throws Exception {
  int diaPago = transaccionGenericoFrom.getCalendarizacion().getFechaAplicacionDia();
  int mesPago = transaccionGenericoFrom.getCalendarizacion().getFechaAplicacionMes();
  int anioPago = transaccionGenericoFrom.getCalendarizacion().getFechaAplicacionAnno();
  int hora = transaccionGenericoFrom.getCalendarizacion().getFechaAplicacionHora();

  //Creamos la cantidad de transacciones especificadas
  int daysInAMonth = FechaUtils.getNumberDaysInAMonth(anioPago, mesPago);
  int diaPagoReal = 0;
  for (int i = 0; i < (transaccionGenericoFrom.getCantTrx() - 1); i++) {
   diaPagoReal = diaPago;
   mesPago += 3;

   if (mesPago > 12) {
    switch (mesPago) {
     case 13:
      mesPago = 1;
      break;
     case 14:
      mesPago = 2;
      break;
     case 15:
      mesPago = 3;
      break;
    }
    anioPago += 1;
   }
   //Obtenemos la cantidad de dias del mes para tener un parametro mas exacto
   daysInAMonth = FechaUtils.getNumberDaysInAMonth(anioPago, mesPago);

   if (diaPagoReal > daysInAMonth) {
    diaPagoReal = daysInAMonth;
   }

   // actualizamos la fecha de la programacion
   transaccionGenericoFrom.getCalendarizacion().setFechaAplicacionDia(diaPagoReal);
   transaccionGenericoFrom.getCalendarizacion().setFechaAplicacionMes(mesPago);
   transaccionGenericoFrom.getCalendarizacion().setFechaAplicacionAnno(anioPago);
   transaccionGenericoFrom.getCalendarizacion().setFechaAplicacionHora(hora);

   transaccionSession.setFechaProgramacionAutomatica(transaccionGenericoFrom.getCalendarizacion().getFechaAsDate().getTime());
   transaccionSession.setEstado(EstadoTransaccion.ESTADO_PROGRAMADA_POR_APLICAR);
   TransaccionUtils.save(logs, transaccionSession, wait);
  }
 }

}