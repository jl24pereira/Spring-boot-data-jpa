package com.fintec.potala.struts.actions;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
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

import com.ba.servicios.integracion.Servicios;
import com.fintec.comunes.Constantes;
import com.fintec.comunes.FechaUtils;
import com.fintec.comunes.LogHelper;
import com.fintec.comunes.Utils;
import com.fintec.potala.bac.dq.AbstractDataQueue;
import com.fintec.potala.bac.dq.CuentaNoExisteException;
import com.fintec.potala.bac.dq.trans.TransactionDispacher;
import com.fintec.potala.bac.dq.trans.multiples.MultiplesCtoPredefNombre;
import com.fintec.potala.corporativa.clazzes.EstadoTransaccion;
import com.fintec.potala.corporativa.clazzes.LineaTransaccion;
import com.fintec.potala.corporativa.clazzes.MQResultado;
import com.fintec.potala.corporativa.clazzes.TipoOperacion;
import com.fintec.potala.corporativa.clazzes.TipoTransaccion;
import com.fintec.potala.corporativa.clazzes.Transaccion;
import com.fintec.potala.corporativa.clazzes.TrxEncExt;
import com.fintec.potala.corporativa.clazzes.Usuario;
import com.fintec.potala.corporativa.services.TransaccionUtils;
import com.fintec.potala.struts.actions.pagos.PagoAFPAction;
import com.fintec.potala.struts.clases.ParametrosPlanilla;
import com.fintec.potala.struts.clases.SessionKeys;
import com.fintec.potala.struts.forms.TransaccionGenericoForm;
import com.fintec.potala.struts.forms.consultas.ConsultaComprobantesForm;
import com.fintec.potala.struts.forms.mantenimientos.CuentasPredefinidasForm;
import com.fintec.potala.struts.forms.pagos.PagaduriaForm;
import com.fintec.potala.struts.forms.pagos.PagoPlanillaForm;
import com.fintec.potala.struts.forms.pagos.PagoPlanillaGenericoForm;
import com.fintec.potala.struts.forms.pagos.PagoProveedoresForm;
import com.fintec.potala.struts.forms.pagos.PagoServicioForm;
import com.fintec.potala.struts.forms.pagos.PagoServicioSinNpeForm;
import com.fintec.potala.struts.forms.transacciones.TransferenciaPropiasForm;
import com.fintec.potala.web.bac.jdbc.db.AFP;
import com.fintec.potala.web.bac.jdbc.db.CodigoTransaccionAS400;
import com.fintec.potala.web.bac.jdbc.db.CodigoTransaccionAS400Utils;
import com.fintec.potala.web.bac.jdbc.db.MyCuenta;
import com.fintec.potala.web.bac.jdbc.sql.MyCuentaSQL;
import com.fintec.potala.web.clases.ObservacionPlanillas;
import com.fintec.sistema.Sistema;

import com.fintec.potala.struts.actions.ServiciosTransaccionesYConsultas;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Session;
import com.fintec.hibernate.HibernateMap;
import com.fintec.potala.corporativa.clazzes.FechasCorteTransaccionPago;
import com.fintec.potala.struts.actions.pagos.PagoAFPAction;
import com.fintec.potala.struts.forms.pagos.PagoAFPForm;

/**
 * Este action es la base para todas las transacciones que se clasifican de la
 * siguiente manera: Simples: Bloqueo de cheque Bloqueo de tarjteta etc. Dobles:
 * Transferencia propias Transferencia a terceros Pago de Servicio Pago de
 * Tarjeta etc. Multiples: Planillas Cargos a Terceros etc. Solicitudes:
 * Solicitud de Carta de Credito Solicitud de transferencia internacional etc.
 * Todas las transacciones cumplen un mismo ciclo Creacion: Se crea la
 * transaccion con los datos validados Confirmacion: El usuario confirma los
 * datos y guarda la transaccion Autorizacion: El autorizador consulta y
 * autoriza la transaccion. Aplicacion o eliminacion: Se aplica o elimina la
 * transaccion. Esta clase se encarga de los dos primeros pasos del ciclo, pero
 * existen algunas transacciones que se aplican directamente sin necesidad de
 * autorizacion. Es decir solo cumplen los pasos 1,2,4, algunas de estas
 * transaccion son RESERVA DE CHEQUE y BLOQUEO TARJETA.
 * 
 * Proceso de una transaccion: 1) La transaccion se crea y se valida.
 * (accion=creacion) 2) Una vez creada el usuario verifica los datos.
 * (accion=confirmacion) 3) Una vez que se confirman entonces se puede aplicar:
 * (accion=aplicacion) 3.1) Guardar la transaccion como pendiente si necesita
 * ser autorizada o es una transaccion calendarizada (programada). 3.2) Aplica
 * la transaccion de una vez. Algunas solicitudes se realizan de esta forma sin
 * necesidad de ser autorizadas.
 * 
 * Todas las transacciones tiene 4 forwards el de creacion, modificacion,
 * confirmacion y resultado El de modificacion solo se utiliza para
 * transacciones con muchas lineas como planillas donde se pueden agregar o
 * eliminar lineas de abono o de cargo hasta cuadrar la planilla y enviarla a
 * confirmacion.
 * 
 * 
 * 
 * @author psaenz
 * @version 1.0
 */
public class TransaccionGenericoAction extends ValidaSessionBaseAction {
	private static final int ACCESO_CORRECTO = 0;

	private static final int ACCESO_INCORRECTO = -1;

	private static final int TRANSACCION_YA_AUTORIZADA = -2;

	private static final int TRANSACCION_YA_APLICADA = -3;

	private static final int TRANSACCION_YA_PROCESADA = -4;

	private static final String DIARIO = "D";

	private static final String SEMANAL = "S";

	private static final String QUINCENAL = "Q";

	private static final String MENSUAL = "M";

	private static final String TRIMESTRAL = "T";

	private static final int MAXIMO_MES = 12;

	/**
	 * Devuelve el forward de creacion Este forward depende del mapping el cual
	 * es diferente para todas las transacciones, por lo tanto si recibimos el
	 * mapping de pago de servicios entonces el forward de "creacion" lo enviara
	 * a la pagina de creacion de un pago de servcicios
	 * 
	 * @param mapping
	 *            Mapping correspondiente a la transaccion que se esta
	 *            utilizando
	 * @return Retorna el forward "crear"
	 */
	public ActionForward getForwardCreacion(ActionMapping mapping) {
		return mapping.findForward("crear");
	}

	/**
	 * Devuelve el forward de confirmacion Este forward depende del mapping el
	 * cual es diferente para todas las transacciones, por lo tanto si recibimos
	 * el mapping de pago de servicios entonces el forward de "confirmar" lo
	 * enviara a la pagina de confirmacion de un pago de servcicios
	 * 
	 * @param mapping
	 *            Mapping correspondiente a la transaccion que se esta
	 *            utilizando
	 * @return Retorna el forward "confirmar"
	 */
	public ActionForward getForwardConfirmacion(ActionMapping mapping) {
		return mapping.findForward("confirmar");
	}

	/**
	 * Devuelve el forward de resultado Este forward depende del mapping el cual
	 * es diferente para todas las transacciones, por lo tanto si recibimos el
	 * mapping de pago de servicios entonces el forward de "resultado" lo
	 * enviara a la pagina de resultado de un pago de servcicios
	 * 
	 * @param mapping
	 *            Mapping correspondiente a la transaccion que se esta
	 *            utilizando
	 * @return Retorna el forward "resultado"
	 */
	public ActionForward getForwardResultado(ActionMapping mapping) {
		return mapping.findForward("resultado");
	}

	/**
	 * Devuelve el forward de error Este forward es el mismo para todas las
	 * transacciones, presenta una pagina con el mensaje de error
	 * 
	 * @param mapping
	 *            Mapping correspondiente a la transaccion que se esta
	 *            utilizando
	 * @return Retorna el forward "error"
	 */
	public ActionForward getForwardError(ActionMapping mapping) {
		return mapping.findForward("error");
	}

	private void saveMessagesAndErrors(HttpServletRequest request,
			ParametrosPlanilla parametros) {
		saveErrors(request, parametros.getErrors());
		saveMessages(request, parametros.getMessages());
	}

	/**
	 * Evalua el estado actual de la transaccion para asegurar una secuencia de
	 * pasos correcta creacion -> confirmacion -> aplicacion creacion ->
	 * modificacion -> confirmacion -> aplicacion (transacciones multiples como
	 * planillas)
	 * 
	 * @param parametros
	 *            Recibe los datos generales de la transaccion, asi como los
	 *            datos del request, la session, y el objeto de mensajes,
	 *            errores y forwards
	 * @return -1 : Acceso incorrecto. Cuando no hay coordinacion entre los
	 *         pasos ejecutados y el paso que se desea ejecutar. Por ejemplo que
	 *         el usuario desea realizar el paso de aplicacion si haber
	 *         ejecutado el paso de confirmacion. 0 : Acceso correcto. Cuando
	 *         toda la coordinacion y el estado de la transaccion concuerdan -2 :
	 *         Transaccion ya autorizada. Cuando la transaccion ya esta aplicada
	 *         o autorizada, esta condicion impide al usuario modificar la
	 *         transaccion.
	 */
	private int estadoValido(ParametrosPlanilla parametros) {
		// Desde el menu el estado llega en cero
		if (parametros.getTransaccionForm().getEstado().equals("0")) {
			parametros.getTransaccionForm().setEstado(
					TransaccionGenericoForm.ESTADO_INICIAL);
		}

		// ----Cualquier estado en null es invalido
		if (parametros.getTransaccionFormSession() == null
				|| parametros.getTransaccionForm() == null) {
			System.out.println("Error en validación de forms");
			System.out.println("parametros.getTransaccionFormSession(): "
					+ parametros.getTransaccionFormSession());
			System.out.println("parametros.getTransaccionForm(): "
					+ parametros.getTransaccionForm());
			return ACCESO_INCORRECTO; // ----Acceso invalido
		}

		// ----Si el estado del form del request es inicial entonces es valido
		if (parametros.getTransaccionForm().getEstado().equals(
				TransaccionGenericoForm.ESTADO_INICIAL)) {
			return ACCESO_CORRECTO; // ----Acceso valido
		}

		// ----Si ya se ejecuto entonces es invalido
		if (parametros.getTransaccionFormSession().getEstado().equals(
				TransaccionGenericoForm.ESTADO_EJECUTADO)) {
			try {
				if (parametros.getTransaccionFormSession().getTransaccion()
						.getEstado().equals(EstadoTransaccion.ESTADO_APLICADA)) {
					return TRANSACCION_YA_APLICADA; // ----Transaccion ya
													// aplicada
				} else if (parametros
						.getTransaccionFormSession()
						.getTransaccion()
						.getEstado()
						.equals(EstadoTransaccion.ESTADO_PENDIENTE_AUTORIZACION)) {
					return TRANSACCION_YA_AUTORIZADA; // ----Transaccion ya
														// autorizada
				} else {
					return TRANSACCION_YA_PROCESADA; // ----Transaccion ya
														// autorizada
				}
			} catch (Exception e) {
				// TODO: la transaccion puede estar null...revisar si este caso
				// sucede
				System.out.println(LogHelper.log(e));
				return TRANSACCION_YA_PROCESADA;
				// ----Transaccion ya autorizada
			}
		}

		// ----Si el estado del request es confirmando y el de la session es
		// menor (creando) entonces es invalido
		if (parametros.getTransaccionForm().getEstado().equals(
				TransaccionGenericoForm.ESTADO_CONFIRMACION)
				&& parametros.getTransaccionFormSession().getEstado().equals(
						TransaccionGenericoForm.ESTADO_CREACION)) {
			System.out.println("Estados de form diferentes");
			System.out.println("parametros.getTransaccionForm().getEstado(): "
					+ parametros.getTransaccionForm().getEstado());
			System.out
					.println("parametros.getTransaccionFormSession().getEstado(): "
							+ parametros.getTransaccionFormSession()
									.getEstado());
			return ACCESO_INCORRECTO; // ----Acceso invalido
		}

		// cpalacios; feb2014; "esta lineas if (("aplicar". " no van comentadas,
		// pero sino no me funcionan localmente.
		if (("aplicar".equals(parametros.getTransaccionForm().getAccion()) || "guardar"
				.equals(parametros.getTransaccionForm().getAccion()))
				&& !parametros.getTransaccionFormSession().getEstado().equals(
						TransaccionGenericoForm.ESTADO_CONFIRMACION)) {
			System.out.println("Accion no concuerda con estado");
			System.out.println("parametros.getTransaccionForm().getAccion(): "
					+ parametros.getTransaccionForm().getAccion());
			System.out
					.println("parametros.getTransaccionFormSession().getEstado(): "
							+ parametros.getTransaccionFormSession()
									.getEstado());
			return ACCESO_INCORRECTO; // ----Acceso invalido
		}
		return ACCESO_CORRECTO; // ----Acceso valido
	}

	/**
	 * Este metodo se encarga de controlar el curso de la transaccion, verifica
	 * el estado y localiza el forward respectivo, por ejemplo si el estado de
	 * la transaccion es el inicial entonces lo envia al forward de "creacion"
	 * luego lo enviara al forward de confirmacion y por ultimo el de resultado.
	 * 
	 * @param mapping
	 *            Mpping de Struts
	 * @param form
	 *            Form con los datos del request
	 * @param request
	 *            Request del usuario
	 * @param response
	 *            Response del usuario
	 * @throws Exception
	 *             Cualquier error
	 * @return Posibles forwars Depende del estado y del curso de la
	 *         transaccion:
	 *         <ul>
	 *         <li>Forward a creacion: Cuando esta ingresando a la opcion desde
	 *         el menu o cuando los valores ingresados son incorrectos</li>
	 *         <li>Forward de confirmacion: Cuando todos los datos son
	 *         correctos entonces lo envia a una pagina para que confirme la
	 *         operacion</li>
	 *         <li>Forward de resultado: Resultado final al guardar la
	 *         transaccion</li>
	 *         </ul>
	 */
	public ActionForward executeAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
		HttpSession session = request.getSession(false);
		Usuario usuario = getUsuario(session);
		session.setAttribute("pendientes", "false");
		String metodo = "executeAction()";
		// ---- Parametros que se comparten por todas las clases que hereden
		// este action
		ParametrosPlanilla parametros = new ParametrosPlanilla( new ActionForward(), new ActionMessages(), new ActionErrors(), new TransaccionGenericoForm(), new TransaccionGenericoForm());
		try {
			Transaccion transaccionSession;
			// ---- Tomamos los forms del request y el de la session
			parametros.setTransaccionForm((TransaccionGenericoForm) form);
			parametros.setTransaccionFormSession((TransaccionGenericoForm) session.getAttribute(parametros.getTransaccionForm().getSessionKey()));
			System.out.println("Accion = " + parametros.getTransaccionForm().getAccion());
			System.out.println(this.getClass().getName()+ ".executeAction.Inicio (Estado = "+ parametros.getTransaccionForm().getEstado() + ")");
			// Tomamos la transaccion en session
			transaccionSession = (parametros.getTransaccionFormSession() != null) ? parametros.getTransaccionFormSession().getTransaccion() : null;
			// ----Si el form en la session es null entonces creamos uno
			if (parametros.getTransaccionFormSession() == null) {
				parametros.setTransaccionFormSession(new TransaccionGenericoForm());
			}
			String accion = parametros.getTransaccionForm().getAccion();
			int acceso = this.estadoValido(parametros);
			if (acceso == ACCESO_CORRECTO) {
				escribirLog(metodo, "Ingreso evaluado como correcto");
				// ----Evaluamos el estado para ver a cual accion se dirige
				// --------------------------------Primer Estado
				// ----Si el estado es inicial entonces el execute es de inicio
				/**********************************************************************************
                ******************************* PRIMER ESTADO *************************************
				******************************* ESTADO INICIAL ************************************
				**********************************************************************************/
				if (parametros.getTransaccionForm().getEstado().equals(TransaccionGenericoForm.ESTADO_INICIAL)) {
					escribirLog(metodo, "Procesando estado inicial");
					try {
						// Verificando si necesita token
						escribirLog(metodo,"Validando si la transaccion es tokenizada");
						if (TransaccionUtils.usuarioUnico(usuario) && TransaccionUtils.esTransaccionPorToken(parametros.getTransaccionForm().getTipoTransaccion())) {
							// Verificando si el usuario en efecto posee el token
                            if (!usuario.getTieneToken()) 
                            parametros.getErrors().add("transacciones",new ActionError("errors.transaccion.tokennecesario"));
						}
					} catch (Exception e) {
						escribirLog(metodo,"Error validando si la transaccion es tokenizada");
						e.printStackTrace();
					}
					// ----Cuando el iniciar entonces limpiamos la session
					parametros.setTransaccionFormSession(new TransaccionGenericoForm());
					// ----Capturamos el forward
					parametros.setForward(this.executeInicio(mapping,parametros.getTransaccionForm(), request, response,parametros));
					parametros.getTransaccionForm().setEstado(TransaccionGenericoForm.ESTADO_CREACION);
					/**
					 * MODIFICADO CHERNANDEZ 2010506 Validación de mostrar
					 * mensaje si la transaccion sera procesada en el siguiente
					 * día habil
					 */
					try {
						escribirLog(metodo,"Validando creacion transaccion ambiente nocturno");
						if (TransaccionUtils.guardarExtensionTransaccion(0,parametros.getTransaccionForm().getTipoTransaccion().getId(), false)
								&& parametros.getErrors().size() <= 0) {
							escribirLog(metodo,"Aplica para mostrar mensaje procesamiento siguiente dia habil");
							parametros.getMessages().add("transacciones",new ActionMessage("mensajes.transaccionprocesada.diahabilsiguiente.creacion"));
						}
						/**
						 * BORRAR ESTA LINEA DESPUES, SOLO ESTA PARA EFECTO DE
						 * PRUEBAS *
						 */
						// parametros.getMessages().add("transacciones", new
						// ActionMessage("mensajes.transaccionprocesada.diahabilsiguiente.creacion"));
					} catch (Exception e) {
						escribirLog(metodo, "Error validando dia habil");
						e.printStackTrace();
					}
					// ----Guardamos los forms en el request y en la session
					request.setAttribute(parametros.getTransaccionForm().getSessionKey(), parametros.getTransaccionForm());
					session.setAttribute(parametros.getTransaccionForm().getSessionKey(), parametros.getTransaccionFormSession());
					// Salvamos los mensajes
					saveMessagesAndErrors(request, parametros);
					return parametros.getForward();
				}
				// --------------------------------Segundo Estado
				/**********************************************************************************
                ****************************** SEGUNDO ESTADO *************************************
				****************************** ESTADO DE CREACION *********************************
				**********************************************************************************/
				if (parametros.getTransaccionForm().getEstado().equals(TransaccionGenericoForm.ESTADO_CREACION)) {
					escribirLog(metodo, "Procesando estado CREACION");
					// ----Hay una excepcion cuando se trata de planillas, donde
					// si se esta creando la transaccion no debemos validar los
					// campos
					ActionErrors errorsForm = new ActionErrors();
					if (!(parametros.getTransaccionForm() instanceof PagoPlanillaGenericoForm)) {
						// ----Validamos el form en el server
						errorsForm = parametros.getTransaccionForm().validate(mapping, request);
					}
					// JMenenedez-20170915
					if ("confirmar".equalsIgnoreCase(parametros.getTransaccionForm().getAccion()) && (parametros.getTransaccionForm() instanceof PagoServicioSinNpeForm)) {
						errorsForm = new ActionErrors();
						// se toma el form de session para agregar los datos
						// capturados en el form de request
						PagoServicioSinNpeForm pagoRequest = (PagoServicioSinNpeForm) parametros.getTransaccionForm();
						PagoServicioSinNpeForm pagoSession = (PagoServicioSinNpeForm) parametros.getTransaccionFormSession();
						pagoSession.setMontoPago(pagoRequest.getMontoPago());
						pagoSession.setAccion(pagoRequest.getAccion());
						pagoSession.setEstado(pagoRequest.getEstado());
						parametros.setTransaccionForm(pagoSession);
					}// Fin-JMenenedez-20170915
					// ----Ahora validamos la fecha y la hora siempre y cuando
					// sea una transaccion calendarizada
					/**** VERIFICAR LA CALENDARIZACION DE LA TRANSACCION (SI SE LE ASIGNO FECHA) *****/
					boolean fechaValida = validarFechaCalendarizada(parametros);
					// validacion para tipo de dato desembolso linea; cpalacios;
					// ojo, debe corregirse mejor el action de transaccion
					// respectiva
					// esto para evitar hacer esta validacion
					if (parametros.getTransaccionForm().getTipoTransaccion().getDescripcion().trim().equals(TipoTransaccion.DESEMBOLSO_EN_LINEA_LC.getDescripcion())) {
						fechaValida = true;
					}
					// INICIO: cpalacios; Dic2013/Ene2014; fechas de corte
					// ISSS/AFP;
					boolean isFechaFechaCorteValida = true;
					if (parametros.getTransaccionForm().getCalendarizada() 
							&& ( parametros.getTransaccionForm().getTipoTransaccion().getDescripcion().trim().equals(TipoTransaccion.PAGO_ISSS.getDescripcion()) 
									|| parametros.getTransaccionForm().getTipoTransaccion().getDescripcion().trim().equals(TipoTransaccion.PAGO_PLANILLA_ISSS_EDUCO.getDescripcion()))
					) {
						isFechaFechaCorteValida = ServiciosTransaccionesYConsultas.fechaCortePagoProgramado_ISSS(55, mapping,parametros, 1, 1);
					} else if (parametros.getTransaccionForm().getCalendarizada()
							&& (parametros.getTransaccionForm().getTipoTransaccion().getDescripcion().trim().equals(TipoTransaccion.PAGO_AFP.getDescripcion()))
					) {
						int tipoCompaniaPago = 0;
						AFP afpVar = (AFP) session.getAttribute(PagoAFPAction.AFP_PAGO_CORTE_SESION);
						String PagoSession = (String) session.getAttribute(PagoAFPAction.AFP_PAGO_PERIODO_SESION);
						if (afpVar != null && PagoSession != null) {
							System.out.println("############# AFP. CODIGO = "
									+ afpVar.getCode() + "   NOMBRE = "
									+ afpVar.getName() + "  NEMO = "
									+ afpVar.getNemo());
							tipoCompaniaPago = afpVar.getCode();
							isFechaFechaCorteValida = ServiciosTransaccionesYConsultas.fechaCortePagoProgramado_AFP(tipoCompaniaPago, mapping,parametros, 1, 1, PagoSession);
						}
					}
					// FIN: cpalacios; Dic2013/Ene2014; fechas de corte
					// ISSS/AFP;
					// if (errorsForm.isEmpty() && fechaValida) {
					/*******************************************************************
					********************************************************************
					***** EJECUCION DE LA CONFIRMACION SEGUN C/TIPO DE TRANSACCION *****
					********************************************************************
					*******************************************************************/
					if (errorsForm.isEmpty() && fechaValida && isFechaFechaCorteValida) { 
						/* validar bien esto, para LOTE U OTRAS TRANSACCIONES DIFERENTES DE  PAGO ISSS */
						// ----Hacemos la confirmacion
						parametros.setForward(this.executeConfirmacion(mapping,parametros.getTransaccionForm(), request,response, parametros));
						// ----Actualizamos campos
						{
							// ----Actualizamos las fechas
							parametros.getTransaccionForm().setFechaActual(parametros.getTransaccionForm().getFechaFromServer());
							parametros.getTransaccionForm().setFechaSistema(parametros.getTransaccionForm().getFechaFromIBS());
							// Validando Transacciones Pendientes de aplicar
							// ----------------------------------------------
							Servicios.validarTransaccionesPendientes(parametros, session, usuario);
							String pendientes = session.getAttribute("pendientes").toString();
							// ----Actualizamos el estado del form
							if (parametros.getErrors().isEmpty() && parametros.getMessages().isEmpty()
									|| (parametros.getErrors().isEmpty() && !parametros.getMessages().isEmpty() && pendientes.equals("true"))) {
								parametros.getTransaccionForm().setEstado(TransaccionGenericoForm.ESTADO_CONFIRMACION);
								// Si es transferencia propias, pregunta si la
								// cuenta a cargar es de credito para mostrar
								// el mensaje de la comision
								if (parametros.getTransaccionForm() instanceof TransferenciaPropiasForm) {
									TransferenciaPropiasForm transferenciaPropiasForm = (TransferenciaPropiasForm) parametros.getTransaccionForm();
									if (transferenciaPropiasForm.getCuenta().esTarjetaCredito()) {
										double porcentajeComision = Servicios.getComisionCuentasPropiasTarjetaCredito(transferenciaPropiasForm.getCuenta().getNumero(),transferenciaPropiasForm.getMonto(),usuario.getUser(), "");
										double comision = (transferenciaPropiasForm.getMonto() * porcentajeComision) / 100;
										DecimalFormat df = new DecimalFormat("0.00");
										parametros.getMessages().add("transferencia",new ActionMessage("mensajes.transferencias.comisionRetiroTarjetaCredito",df.format(comision)+ "",porcentajeComision+ ""));
									}
								}
								// Inicio: cpalacios; Dic2013; fechas de corte
								// ISSS/AFP
								int tipoCompaniaPago = 0;
								AFP afpVar = (AFP) session.getAttribute(PagoAFPAction.AFP_PAGO_CORTE_SESION);
								String PagoSession = (String) session.getAttribute(PagoAFPAction.AFP_PAGO_PERIODO_SESION);
								if (afpVar != null) {
									System.out.println("############# AFP. CODIGO = "+ afpVar.getCode()+ "   NOMBRE = "+ afpVar.getName()+ "  NEMO = "+ afpVar.getNemo());
									tipoCompaniaPago = afpVar.getCode();
									// SE SETEA SI ES PAGAR AFP(UPISSS, CONFIA,
									// CRECER, INPEP) O IPSFA
									if (parametros.getTransaccionForm().getTipoTransaccion().getDescripcion().trim().equals(TipoTransaccion.PAGO_AFP.getDescripcion())) {
										PagoAFPForm variableForm = (PagoAFPForm) form;
										if (ServiciosTransaccionesYConsultas.fechaCortePago_AFP_Transacciones(tipoCompaniaPago,mapping, parametros, 1,1, PagoSession) == false
												|| !errorsForm.isEmpty()) {
											// parametros.getErrors().add("pagoafp",
											// new
											// ActionError("mensaje.simple","ERROR
											// CON CODIGO AFP OBTENIDO O
											// CONFIGURADO, CONSULTE CON EL
											// ADMINISTRADOR DEL SISTEMA"));
											// parametros.setForward(getForwardError(mapping));
											parametros.setForward(this.executeInicio(mapping,parametros.getTransaccionForm(),request,response,parametros));
											parametros.setErrors(errorsForm);
										}
									}
								}
								/*
								 * SI ES PAGO DE ISSS EL VALOR DE
								 * tipoCompaniaPago = 55
								 */
								if (parametros.getTransaccionForm().getTipoTransaccion().getDescripcion().trim().equals(TipoTransaccion.PAGO_PLANILLA_ISSS_EDUCO.getDescripcion())) {
									int pasoBandera = 1;
									int pasoTransaccion = 1;
									if (ServiciosTransaccionesYConsultas.fechaCortePago_ISSS_Transacciones(55, mapping, parametros,pasoBandera,pasoTransaccion) == false
											|| !errorsForm.isEmpty()) {
										parametros.setForward(this.executeInicio(mapping,parametros.getTransaccionForm(),request,response,parametros));
										parametros.setErrors(errorsForm);
									}
								}
								// Fin: cpalacios; Dic2013; fechas de corte
								// ISSS/AFP
							}

							// ----Copiamos los datos del request a la session y
							// guardamos la session
							PropertyUtils.copyProperties(parametros.getTransaccionFormSession(), parametros.getTransaccionForm());
							session.setAttribute(parametros.getTransaccionForm().getSessionKey(),parametros.getTransaccionFormSession());
						}
					} else {
						// ----Datos del form incorrectos
						System.out.println(this.getClass().getName()+ ".form con datos incorrectos");
						// ----Si el form no se valido entonces volvemos a
						// iniciar la transaccion
						parametros.setForward(this.executeInicio(mapping,parametros.getTransaccionForm(), request,response, parametros));
						parametros.setErrors(errorsForm);
						// PropertyUtils.copyProperties(parametros.getTransaccionForm(),
						// parametros.getTransaccionFormSession());
					}
					// Guardamos el form en el request
					request.setAttribute(parametros.getTransaccionForm().getSessionKey(), parametros.getTransaccionForm());
					// ----Salvamos los errores y los devolvemos a la pagina de
					// inicio
					saveMessagesAndErrors(request, parametros);
					escribirLog(metodo, "Haciendo forward a "+ parametros.getForward().getPath());
					return parametros.getForward();
				}
                /**********************************************************************************
                ***************************** TERCER ESTADO ***************************************
				***************************** PROCESAR CONFIRMACION *******************************
				**********************************************************************************/
				if (parametros.getTransaccionForm().getEstado().equals(TransaccionGenericoForm.ESTADO_CONFIRMACION)) {
					escribirLog(metodo, "Procesando estado CONFIRMACION");
					// Del estado de confirmacion podemos ir al estado inicial o
					// al estado de ejecucion dependiento del accion
					// ----Si no es "aplicar" ni "guardar" entonces se toma como
					// "corregir"
					if (!("aplicar".equals(parametros.getTransaccionForm().getAccion()))
							&& !("guardar".equals(parametros.getTransaccionForm().getAccion()))
							&& !("procesar".equals(parametros.getTransaccionForm().getAccion()))
							&& !("guardarValidarImpuesto".equals(parametros.getTransaccionForm().getAccion()))) {
						/**
						 * MODIFICADO CHERNANDEZ 2010506 Validación de mostrar
						 * mensaje si la transaccion sera procesada en el
						 * siguiente día habil
						 */
						try {
							System.out.println(this.getClass().getName()+ ".executeCorregir.inicio. Validando creacion transaccion ambiente nocturno");
							if (TransaccionUtils.guardarExtensionTransaccion(0,parametros.getTransaccionForm().getTipoTransaccion().getId(),false)
									&& !parametros.getTransaccionForm().getTipoTransaccion().equals(TipoTransaccion.PAGO_PLANILLA)
									&& !parametros.getTransaccionForm().getTipoTransaccion().equals(TipoTransaccion.PAGO_PROVEEDORES)
									&& !parametros.getTransaccionForm().getTipoTransaccion().equals(TipoTransaccion.PAGADURIAS)
									&& !parametros.getTransaccionForm().getTipoTransaccion().equals(TipoTransaccion.PAGO_PLANILLA_BONIFICACION)
									&& !parametros.getTransaccionForm().getTipoTransaccion().equals(TipoTransaccion.ABONO_PENSIONADOS)
									&& !parametros.getTransaccionForm().getTipoTransaccion().equals(TipoTransaccion.CARGA_BOLETA_PAGO)
									&& !parametros.getTransaccionForm().getTipoTransaccion().equals(TipoTransaccion.LIQUIDACION_FACTURACION_POS)) {
								System.out.println(this.getClass().getName()+ ".executeCorregir.inicio. Aplica para mostrar mensaje procesamiento siguiente dia habil");
								parametros.getMessages().add("transacciones",new ActionMessage("mensajes.transaccionprocesada.diahabilsiguiente.creacion"));
							}
							/**
							 * BORRAR ESTA LINEA DESPUES, SOLO ESTA PARA EFECTO
							 * DE PRUEBAS *
							 */
							// parametros.getMessages().add("transacciones", new
							// ActionMessage("mensajes.transaccionprocesada.diahabilsiguiente.creacion"));
						} catch (Exception e) {
							System.out.println(this.getClass().getName()+ ".executeCorregir.inicio. Error validando dia habil");
							e.printStackTrace();
						}

						System.out.println(this.getClass().getName()+ ".executeCorregir.inicio");
						parametros.getTransaccionFormSession().setEstado(TransaccionGenericoForm.ESTADO_CREACION);
						parametros.setForward(this.executeCorregir(mapping,parametros.getTransaccionForm(), request,response, parametros));
						System.out.println(this.getClass().getName()+ ".executeCorregir.fin\n");
					}

					if ("ConfirmarAplicar".equals(parametros.getTransaccionForm().getAccion())) {
						// ----Hay una excepcion cuando se trata de planillas,
						// donde si se esta creando la transaccion no debemos
						// validar los campos
						ActionErrors errorsForm = new ActionErrors();
						if (!(parametros.getTransaccionForm() instanceof PagoPlanillaGenericoForm)) {
							// ----Validamos el form en el server
							request.setAttribute("validacion", "true");
							errorsForm = parametros.getTransaccionForm().validate(mapping, request);
						}
						if (errorsForm.isEmpty()) {
							parametros.setForward(this.executeConfirmacion(mapping, parametros.getTransaccionForm(),request, response, parametros));
						}
						// ----Ahora validamos la fecha y la hora siempre y
						// cuando sea una transaccion calendarizada
						boolean fechaValida = validarFechaCalendarizada(parametros);
						if (errorsForm.isEmpty() && fechaValida && parametros.getForward().getName().equals("resultado")) {
							System.out.println(this.getClass().getName()+ ".executeAplicar.inicio");
							// Para aplicarla tenemos que guardarla
							// Guarda la transaccion y espera que sea guardada
							// en su totalidad
							saveTransaccion(parametros, usuario, true);
							// Si es programada entonces NO se aplica
							if (!parametros.getTransaccionFormSession().getCalendarizada()) {
								TransactionDispacher.execute(transaccionSession, usuario);
							}
							// Cargamos el mensaje de error o de exito
							loadMensajeResultado(parametros);
							// Vamos a la pantalla de resultado y comprobante
							parametros.setForward(getForwardResultado(mapping));
							// Guardamos un form de comprobantes porque despues
							// de que se ejecute la transaccion debemos seguir
							// para ver el comprobante
							ConsultaComprobantesForm comprobanteForm = new ConsultaComprobantesForm();
							comprobanteForm.setTransaccion(parametros.getTransaccionFormSession().getTransaccion());
							session.setAttribute(SessionKeys.KEY_COMPROBANTES,comprobanteForm);
						} else {
							// ----Datos del form incorrectos
							System.out.println(this.getClass().getName()+ ".form con datos incorrectos");
							// ----Si el form no se valido entonces volvemos a
							// iniciar la transaccion
							parametros.setErrors(errorsForm);
							parametros.setForward(this.executeInicio(mapping,parametros.getTransaccionForm(), request,response, parametros));
							ActionErrors errorsFormClean = new ActionErrors();
							parametros.setErrors(errorsFormClean);
							// PropertyUtils.copyProperties(parametros.getTransaccionForm(),
							// parametros.getTransaccionFormSession());
						}
					}

					if ("aplicar".equals(parametros.getTransaccionForm().getAccion())) {
						System.out.println(this.getClass().getName() + ".executeAplicar.inicio");
						// 20101001: Validación de planillas, para que no sean
						// procesadas planillas descuadradas entre cargos y
						// abonos.
						if (parametros.getTransaccionForm().getTipoTransaccion().getTipoOperacion().equals(TipoOperacion.MULTIPLE)
								&& transaccionSession.getDiferencia() != 0
								&& validaCuadreCargosAbonos(parametros.getTransaccionForm().getTipoTransaccion())) {
							// ----Vamos a la pantalla de confirmacion por
							// problemas en validación
							parametros.setForward(getForwardConfirmacion(mapping));
							// ----Enviamos un mensaje de "error no cuadra
							// planilla"
							parametros.getMessages().add("transacciones",new ActionMessage("mensaje.planilla.nocuadra"));
						} else {
							// Para aplicarla tenemos que guardarla
							// Guarda la transaccion y espera que sea guardada
							// en su totalidad
							saveTransaccion(parametros, usuario, true);
							// Si es programada entonces NO se aplica
							if (!parametros.getTransaccionFormSession().getCalendarizada()) {
								TransactionDispacher.execute(transaccionSession, usuario);
							}
							// Cargamos el mensaje de error o de exito
							loadMensajeResultado(parametros);
							// Vamos a la pantalla de resultado y comprobante
							parametros.setForward(getForwardResultado(mapping));
							// Si es transaccion propia hecha con tarjeta de
							// credito se mostrara el mensaje
							if (parametros.getTransaccionFormSession() instanceof TransferenciaPropiasForm) {
								TransferenciaPropiasForm transferenciaPropiasForm = (TransferenciaPropiasForm) parametros.getTransaccionFormSession();
								if (transferenciaPropiasForm.getCuenta().esTarjetaCredito()
										&& parametros.getTransaccionFormSession().getTransaccion().getEstado().equals(EstadoTransaccion.ESTADO_APLICADA)) {
									parametros.getMessages().add("transferencia",new ActionMessage("mensajes.transferencias.resultadoRetiroTarjetaCredito"));
								}
							}
							// Guardamos un form de comprobantes porque despues
							// de que se ejecute la transaccion debemos seguir
							// para ver el comprobante
							ConsultaComprobantesForm comprobanteForm = new ConsultaComprobantesForm();
							comprobanteForm.setTransaccion(parametros.getTransaccionFormSession().getTransaccion());
							session.setAttribute(SessionKeys.KEY_COMPROBANTES,comprobanteForm);
						}
						// INICIO: cpalacios; Dic2013/Ene2014; fechas de corte
						// ISSS/AFP;
						if (parametros.getTransaccionForm().getTipoTransaccion().getDescripcion().trim().equals(TipoTransaccion.PAGO_PLANILLA_ISSS_EDUCO.getDescripcion())) {
							int pasoBandera = 1;
							int pasoTransaccion = 1;
							ServiciosTransaccionesYConsultas.fechaCortePago_ISSS_Transacciones(55,mapping, parametros, pasoBandera,pasoTransaccion);
						}
						// FIN: cpalacios; Dic2013/Ene2014; fechas de corte
						// ISSS/AFP;
					}

					// Nueva accion para aquellas transacciones que es necesario
					// validar el impuesto
					if ("guardarValidarImpuesto".equals(parametros.getTransaccionForm().getAccion())) {
						System.out.println(this.getClass().getName()+ ".guardarValidarImpuesto.inicio");
						if (cobrarImpuesto(parametros, request)) {
							parametros.getTransaccionForm().setCobroImpuesto(true);
							parametros.getTransaccionFormSession().setCobroImpuesto(true);
							parametros.setForward(getForwardConfirmacion(mapping));
						} else {
							parametros.getTransaccionForm().setCobroImpuesto(false);
							parametros.getTransaccionFormSession().setCobroImpuesto(false);
							parametros.getTransaccionForm().setAccion("guardar");
						}
					}

					if ("guardar".equals(parametros.getTransaccionForm().getAccion())) {
						System.out.println(this.getClass().getName()+ ".executeGuardar.inicio");
						// INICIO: cpalacios; Dic2013/Ene2014; fechas de corte
						// ISSS/AFP;
						if (parametros.getTransaccionForm().getTipoTransaccion().getDescripcion().trim().equals(TipoTransaccion.PAGO_PLANILLA_ISSS_EDUCO.getDescripcion())) {
							int pasoBandera = 1;
							int pasoTransaccion = 2;
							if (ServiciosTransaccionesYConsultas.fechaCortePago_ISSS_Transacciones(55,mapping, parametros, pasoBandera,pasoTransaccion) == false) {
								// Guardamos los forms
								PropertyUtils.copyProperties(parametros.getTransaccionForm(), parametros.getTransaccionFormSession());
								request.setAttribute(parametros.getTransaccionForm().getSessionKey(),parametros.getTransaccionForm());
								session.setAttribute(parametros.getTransaccionForm().getSessionKey(),parametros.getTransaccionFormSession());
								// ----Guardamos errores y mensajes
								saveMessagesAndErrors(request, parametros);
								System.out.println("Forward to "+ parametros.getForward().getPath());
								if (!parametros.getErrors().isEmpty()) {
									saveErrors(request, parametros.getErrors());
									parametros.setForward(mapping.findForward("error"));
								}
								return parametros.getForward();
							}
						} else if (parametros.getTransaccionForm().getTipoTransaccion().getDescripcion().trim().equals(TipoTransaccion.PAGO_AFP.getDescripcion())) {
							int tipoCompaniaPago = 0;
							AFP afpVar = (AFP) session.getAttribute(PagoAFPAction.AFP_PAGO_CORTE_SESION);
							String PagoSession = (String) session.getAttribute(PagoAFPAction.AFP_PAGO_PERIODO_SESION);
							if (afpVar != null) {
								tipoCompaniaPago = afpVar.getCode();
								PagoAFPForm variableForm = (PagoAFPForm) form;
								if (ServiciosTransaccionesYConsultas.fechaCortePago_AFP_Transacciones(tipoCompaniaPago, mapping,parametros, 1, 2, PagoSession) == false) {
									// Guardamos los forms
									PropertyUtils.copyProperties(parametros.getTransaccionForm(), parametros.getTransaccionFormSession());
									request.setAttribute(parametros.getTransaccionForm().getSessionKey(), parametros.getTransaccionForm());
									session.setAttribute(parametros.getTransaccionForm().getSessionKey(), parametros.getTransaccionFormSession());
									// ----Guardamos errores y mensajes
									saveMessagesAndErrors(request, parametros);
									if (!parametros.getErrors().isEmpty()) {
										saveErrors(request, parametros.getErrors());
										parametros.setForward(mapping.findForward("error"));
									}
									return parametros.getForward();
								}
							}
						}
						// FIN: cpalacios; Dic2013/Ene2014; fechas de corte
						// ISSS/AFP;

						// AFP
						// 20101001: Validación de planillas, para que no sean
						// procesadas planillas descuadradas entre cargos y
						// abonos.
						if (Servicios.limitesValido(usuario.getCliente(),parametros) == "000") {
							if (parametros.getTransaccionForm().getTipoTransaccion().getTipoOperacion().equals(TipoOperacion.MULTIPLE)
									&& transaccionSession.getDiferencia() != 0
									&& validaCuadreCargosAbonos(parametrosgetTransaccionForm().getTipoTransaccion())) {
								// ----Vamos a la pantalla de confirmacion por
								// problemas en validación
								parametros.setForward(getForwardConfirmacion(mapping));
								// ----Enviamos un mensaje de "error no cuadra
								// planilla"
								parametros.getMessages().add("transacciones",new ActionMessage("mensaje.planilla.nocuadra"));
							} else {
								if (parametros.getTransaccionForm() instanceof CuentasPredefinidasForm) {
									CuentasPredefinidasForm cuentasPredefinidasForm = null;
									if (parametros.getTransaccionFormSession().getTipoTransaccion().equals(TipoTransaccion.ELIMINAR_CUENTAS_PREDEFINIDAS)) {
										cuentasPredefinidasForm = (CuentasPredefinidasForm) parametros.getTransaccionFormSession();
									} else {
										cuentasPredefinidasForm = (CuentasPredefinidasForm) parametros.getTransaccionForm();
									}
									Serializable objs[] = {cuentasPredefinidasForm.getConceptoPago(),cuentasPredefinidasForm.getCuentaAsociada() };
									parametros.getTransaccionFormSession().getTransaccion().setMemoAsBytes(Utils.writeObjects(objs));
								}
								beforeSaveTransacccion(mapping, parametros.getTransaccionForm(), request,response, parametros);
								saveTransaccion(parametros, usuario, true);
								// ----Vamos a la pantalla de resultado
								parametros.setForward(this.executeGuardar(mapping, parametros.getTransaccionForm(), request,response, parametros));
								// ----Enviamos un mensaje de "en proceso"
								parametros.getMessages().add("transacciones",new ActionMessage("mensaje.pagos.proceso"));
								// Si es transaccion propia hecha con tarjeta de
								// credito se mostrara el mensaje
								if (parametros.getTransaccionFormSession() instanceof TransferenciaPropiasForm) {
									TransferenciaPropiasForm transferenciaPropiasForm = (TransferenciaPropiasForm) parametros.getTransaccionFormSession();
									if (transferenciaPropiasForm.getCuenta().esTarjetaCredito()) {
										parametros.getMessages().add("transferencia",new ActionMessage("mensajes.transferencias.resultadoRetiroTarjetaCredito"));
									}
								}
								if (parametros.getTransaccionForm() instanceof CuentasPredefinidasForm) {
									// envia el archivo para obtener nombre
									// segun banco por medio del numero de
									// cuenta
									MultiplesCtoPredefNombre multiplesCtoPredefNombre = new MultiplesCtoPredefNombre(parametros.getTransaccionFormSession().getTransaccion());
									multiplesCtoPredefNombre.enviarArchivo();
								}
							}
						} else {
							// Guardamos los forms
							PropertyUtils.copyProperties(parametros.getTransaccionForm(), parametros.getTransaccionFormSession());
							request.setAttribute(parametros.getTransaccionForm().getSessionKey(),parametros.getTransaccionForm());
							session.setAttribute(parametros.getTransaccionForm().getSessionKey(),parametros.getTransaccionFormSession());
							// ----Guardamos errores y mensajes
							saveMessagesAndErrors(request, parametros);
							if (!parametros.getErrors().isEmpty()) {
								saveErrors(request, parametros.getErrors());
								parametros.setForward(mapping.findForward("error"));
							}
							return parametros.getForward();
						}
					}

					if ("procesar".equals(parametros.getTransaccionForm().getAccion())) {
						// Vamos a la pantalla de resultado y comprobante
						parametros.setForward(getForwardResultado(mapping));
					}
					// ----Guardamos los forms
					PropertyUtils.copyProperties(parametros.getTransaccionForm(), parametros.getTransaccionFormSession());
					request.setAttribute(parametros.getTransaccionForm().getSessionKey(), parametros.getTransaccionForm());
					session.setAttribute(parametros.getTransaccionForm().getSessionKey(), parametros.getTransaccionFormSession());
					// ----Guardamos errores y mensajes
					saveMessagesAndErrors(request, parametros);
					System.out.println("Forward to "+ parametros.getForward().getPath());
					if (!parametros.getErrors().isEmpty()) {
						saveErrors(request, parametros.getErrors());
						parametros.setForward(mapping.findForward("error"));
					}

					if (parametros.getForward().equals(getForwardResultado(mapping))) {

						if (parametros.getTransaccionForm() instanceof PagoPlanillaForm
								|| parametros.getTransaccionForm() instanceof PagaduriaForm
								|| parametros.getTransaccionForm() instanceof PagoProveedoresForm)
							System.out.println("eliminar atributo de session "+ parametros.getTransaccionForm().getSessionKey());
						session.removeAttribute(parametros.getTransaccionForm().getSessionKey());
					}
					return parametros.getForward();
				}
			} else {
				escribirLog(metodo, "Ingreso evaluado como INcorrecto");
				System.out.println("Valor de retorno acceso: " + acceso);
				if (acceso == TRANSACCION_YA_AUTORIZADA) {
					System.out.println(" La transaccion ya fue enviada");
					// ----Transaccion ya autorizada
					parametros.getErrors().add("transacciones",new ActionError("errors.transaccion.porautorizar"));
				} else {
					if (acceso == TRANSACCION_YA_APLICADA) {
						System.out.println(" La transaccion ya fue aplicada");
						// ----Transaccion ya aplicada
						parametros.getErrors().add("transacciones",
								new ActionError("errors.transaccion.aplicada"));
					} else {
						if (acceso == TRANSACCION_YA_PROCESADA) {
							System.out.println(" La transaccion ya fue procesada");
							// ----Transaccion ya aplicada
							parametros.getErrors().add("transacciones",new ActionError("errors.transaccion.procesada"));
						} else {
							System.out.println(" El estado de la transaccion es invalido");
							parametros.getErrors().add("transacciones",new ActionError("errors.general.invalidaccess"));
						}
					}
					saveErrors(request, parametros.getErrors());
				}
			}
		} catch (Exception e) {
			mostrarErrorInesperado(e, mapping, parametros.getErrors());
			System.out.println(LogHelper.log(e));
		}

		if (!parametros.getErrors().isEmpty()) {
			saveErrors(request, parametros.getErrors());
			parametros.setForward(mapping.findForward("error"));
		} else {
			parametros.setForward(mapping.findForward("success"));
		}

		return parametros.getForward();
	}

	/**
	 * Determina si hay que cobrar impuesto
	 * 
	 * @param parametros
	 * @param request
	 * @return
	 */
	protected boolean cobrarImpuesto(ParametrosPlanilla parametros,
			HttpServletRequest request) {
		// Validar si aplica validación, debe ser una propiedad del sistema
		boolean validarImpuesto = Sistema
				.getPropertyAsBoolean(Constantes.VALIDAR_IMPUESTO_TRXS_ELECTRONICAS);
		System.out.println("VALIDAR_IMPUESTO_TRXS_ELECTRONICAS : "
				+ validarImpuesto);
		if (validarImpuesto) {
			// validar que si es Cliente exento, devolver false
			HttpSession session = request.getSession(false);
			Usuario usuario = getUsuario(session);
			long cliente = usuario.getCliente().getId();
			String numeroCliente = Long.toString(cliente);
			if (!Servicios.validarClienteExentoImpuesto(numeroCliente, usuario
					.getUsuarioAsString(), request.getRemoteAddr())) {
				// Transaccion transaccion=
				// parametros.getTransaccionFormSession().getTransaccion();
				// validamos que transacción tiene un solo cargo
				validarImpuesto = cargosCobrarImpuesto(parametros, request) > 0;
			} else {
				validarImpuesto = false;
			}

		}
		return validarImpuesto;
	}

	/**
	 * Se encarga de determinar si se realizará la validación de impuesto,
	 * consume el servicio de validación y devuelve los 0, si no hay cobro de
	 * impuesto, 1 o 2, si hay cargos con cobro de impuesto. Solo se consume el
	 * servicio si hay un solo cargo mayor al limite exento, si hay más de uno
	 * se asume que se cobrará el impuesto.
	 * 
	 * @param transaccion
	 * @return
	 */
	public int cargosCobrarImpuesto(ParametrosPlanilla parametros,
			HttpServletRequest request) {
		Transaccion transaccion = parametros.getTransaccionForm()
				.getTransaccion();
		List lineas = parametros.getTransaccionFormSession().getTransaccion()
				.getLineaTransaccionList();
		int size = lineas.size();
		int cargosMayorLimite = 0;
		// Se obtiene elvalor del limite exnto de impuesto
		String limteS = Sistema
				.getProperty(Constantes.MONTO_LIMITE_EXENTO_IMPUESTO);
		BigDecimal limite = null;
		try {
			limite = new BigDecimal(limteS.trim()).setScale(2,
					BigDecimal.ROUND_HALF_UP);
		} catch (Exception e) {
			limite = new BigDecimal("0");
		}
		System.out
				.println("TransaccionGenericoAction.cargosCobrarImpuesto(), limite exento: "
						+ limite.doubleValue());
		// Verificamos si hay más de un cargo mayor al limite exento
		LineaTransaccion lineaTrx = null;
		if (!parametros.getTransaccionFormSession().isCargosMultiples()) {
			for (int i = 0; i < size; i++) {
				LineaTransaccion linea = (LineaTransaccion) lineas.get(i);
				if (linea.esDebito()) {
					// Se incrementa la variable solo si el monto de la linea es
					// mayor al limite exento
					cargosMayorLimite += (linea.getMonto() > limite
							.doubleValue() ? 1 : 0);
					if (cargosMayorLimite == 1) {
						lineaTrx = linea;
					}
					if (cargosMayorLimite > 1) {
						break;
					}
				}
			}
			System.out
					.println("TransaccionGenericoAction.cargosCobrarImpuesto(), cargosMayorLimite: "
							+ cargosMayorLimite);
			// Verificamos si hay un solo cargo mayor al limite, para realizar
			// la validación
			if (cargosMayorLimite == 1 && lineaTrx != null) {
				// consumir servicio
				// Se obtiene el codigo de transaccion
				MyCuenta myCuenta = MyCuentaSQL.getMyCuentaAS400(lineaTrx
						.getCuentaAsString());
				if (myCuenta == null) {
					throw new CuentaNoExisteException(
							"CUENTA, para validar impuesto: "
									+ lineaTrx.getCuentaAsString()
									+ ", no la encontramos en db");
				} else {
					// Buscamos el codigo de transaccion correspondiente
					// solamente los abonos pueden ser
					System.out
							.println("TransaccionGenericoAction.cargosCobrarImpuesto() valores transaccion = "
									+ parametros.getTransaccionForm()
											.getTipoTransaccion()
									+ ", "
									+ lineaTrx.getTipo());
					transaccion.setTipoTransaccion(parametros
							.getTransaccionForm().getTipoTransaccion());
					lineaTrx.setTransaccion(transaccion);

					// Tipo de cuenta Cargo
					String p_tipoCuentaCargo = "";
					String p_tipoServicio = "";
					TipoTransaccion tipoTrx = transaccion.getTipoTransaccion();
					if (tipoTrx.equals(TipoTransaccion.PAGO_PLANILLA)
							|| tipoTrx
									.equals(TipoTransaccion.PAGO_PLANILLA_BONIFICACION)
							|| tipoTrx
									.equals(TipoTransaccion.PAGO_PLANILLA_ACH)
							|| tipoTrx.equals(TipoTransaccion.PAGO_PROVEEDORES)
							|| tipoTrx
									.equals(TipoTransaccion.PAGO_PROVEEDORES_AED)
							|| tipoTrx
									.equals(TipoTransaccion.LIQUIDACION_FACTURACION_POS)
							|| tipoTrx
									.equals(TipoTransaccion.PAGO_PROVEEDORES_ACH)) {
						p_tipoCuentaCargo = "PL";
						p_tipoServicio = "03";
					} else if (tipoTrx
							.equals(TipoTransaccion.CARGOS_AUTOMATICOS)
							|| tipoTrx
									.equals(TipoTransaccion.CARGOS_A_TERCEROS)) {
						p_tipoCuentaCargo = "CA";
						p_tipoServicio = "04";
					} else if (tipoTrx.equals(TipoTransaccion.PAGO_SERVICIOS)) {
						p_tipoCuentaCargo = "SE";
						p_tipoServicio = "02";
					} else if (tipoTrx
							.equals(TipoTransaccion.TRANSF_TERCEROS_ACH)) {
						p_tipoServicio = "EF";
						if (myCuenta.getProducto() >= AbstractDataQueue.CUENTA_AHORRO) {
							p_tipoCuentaCargo = "01";
						} else {
							p_tipoCuentaCargo = "02";
						}
					} else { // Se asume que es transferencia
						if (myCuenta.getProducto() >= AbstractDataQueue.CUENTA_AHORRO) {
							p_tipoCuentaCargo = "01";
						} else {
							p_tipoCuentaCargo = "02";
						}
						p_tipoServicio = "01";
					}
					HttpSession session = request.getSession(false);
					Usuario usuario = getUsuario(session);
					Hashtable datos = Servicios.consultaImpuesto("0", "",
							lineaTrx.getMonto(), lineaTrx.getCuentaAsString(),
							p_tipoCuentaCargo, "", "", "C", usuario
									.getUsuarioAsString(), request
									.getRemoteAddr(), p_tipoServicio);
					// if(datos!=null && !datos.isEmpty()){
					String aplicaImpuesto = (String) datos
							.get("aplicaImpuesto");
					if ("N".equals(aplicaImpuesto)) {
						cargosMayorLimite = 0;
					}
					// }

				}
			}

		} else {// Cargos multiples
			// Se debe validar que haya al menos un abono mayor al limite
			for (int i = 0; i < size; i++) {
				LineaTransaccion linea = (LineaTransaccion) lineas.get(i);
				if (linea.esCredito()) {
					// Se incrementa la variable solo si el monto de la linea es
					// mayor al limite exento
					cargosMayorLimite += (linea.getMonto() > limite
							.doubleValue() ? 1 : 0);

					if (cargosMayorLimite > 0) {
						break;
					}
				}
			}
		}
		return cargosMayorLimite;
	}

	/**
	 * Agrega campo extensión para indicar que la transacción esta sujeta a
	 * cobro de impuesto
	 * 
	 * @param trx
	 */
	public void agregarCampoExtImpuesto(Transaccion trx) {
		TrxEncExt campo = new TrxEncExt();
		campo.setCampo(com.ba.potala.util.Constantes.CAMPO_EXT_COBRO_IMPUESTO);
		campo.setCorrel(0);
		campo.setValor(com.ba.potala.util.Constantes.VALOR_COBRO_IMPUESTO);
		agregarCampoEncabezadoSiNoExiste(campo, trx);
	}

	/**
	 * Añade un campo solo si no existe (no actualiza)
	 * 
	 * @param campo
	 * @param trx
	 */
	public void agregarCampoEncabezadoSiNoExiste(TrxEncExt campo,
			Transaccion trx) {
		if (campo != null && trx != null) {
			if (trx.getCamposExtension() == null) {
				trx.setCamposExtension(new ArrayList());
				trx.getCamposExtension().add(campo);
			} else {
				boolean existe = false;
				for (int x = 0; x < trx.getCamposExtension().size(); x++) {
					if (campo.getCampo().equals(
							((TrxEncExt) trx.getCamposExtension().get(x))
									.getCampo())) {
						existe = true;
						break;
					}
				}
				// Solo añade si no existe
				if (!existe) {
					trx.getCamposExtension().add(campo);
				}
			}
		}
	}

	private void loadMensajeResultado(ParametrosPlanilla parametros) {
		final int LINEA_PROCESADA = 0;

		// Si es una planilla entonces enviamos un mensaje de que esta en
		// proceso
		if (parametros.getTransaccionFormSession().getTransaccion()
				.getTipoTransaccion().getTipoOperacion().equals(
						TipoOperacion.MULTIPLE)) {
			parametros.getMessages().add(
					"enproceso",
					new ActionMessage("mensaje.simple",
							"Transaccion en proceso"));
			return;
		} else {
			String mensaje;
			LineaTransaccion linea;
			Iterator iter = parametros.getTransaccionFormSession()
					.getTransaccion().getLineaTransaccionList().iterator();
			while (iter.hasNext()) {
				linea = (LineaTransaccion) iter.next();

				// Si fue procesada entonces guardamos el numero de referencia
				if (linea.getResultado() == LINEA_PROCESADA) {
					parametros.getTransaccionFormSession()
							.setReferenciaOperacion(
									linea.getNumeroComprobante());
				}

				if (parametros.getTransaccionFormSession().getTipoTransaccion()
						.equals(TipoTransaccion.RESERVA_CHEQUE)) {
					// Si proceso la linea y no la aplico entonces enviamos el
					// mensaje de rechazo
					if (linea.getResultado() != LINEA_PROCESADA
							&& linea.getResultado() == MQResultado.NO_PROCESADO) {
						parametros.getMessages().add(
								"enproceso",
								new ActionMessage("mensaje.simple", linea
										.getMensajeDevolucionAsString()));
						return;
					}
				}

				// Si proceso la linea y no la aplico entonces enviamos el
				// mensaje de rechazo
				if (linea.getResultado() != LINEA_PROCESADA
						&& linea.getResultado() != MQResultado.NO_PROCESADO) {
					parametros.getMessages().add(
							"enproceso",
							new ActionMessage("mensaje.simple", linea
									.getMensajeDevolucionAsString()));
					return;
				}
			}
		}
		if (parametros.getTransaccionFormSession().getTipoTransaccion().equals(
				TipoTransaccion.SUSPENSION_CHEQUES)) {
			parametros.getMessages().add(
					"enproceso",
					new ActionMessage("mensaje.simple",
							"Cheque suspendido satisfactoriamente"));
		} else if (parametros.getTransaccionFormSession().getTipoTransaccion()
				.equals(TipoTransaccion.RESERVA_CHEQUE)) {
			parametros.getMessages().add(
					"enproceso",
					new ActionMessage("mensaje.simple",
							"Cheque reservado satisfactoriamente"));
		} else {
			// Si no es planilla, y se aplico bien entonces podemos ver los
			// comprobantes
			parametros.getMessages().add(
					"enproceso",
					new ActionMessage("mensaje.simple",
							"Transacci&oacute;n aplicada"));
		}
	}

	private boolean validarFechaCalendarizada(ParametrosPlanilla parametros) {
		// ----Ahora validamos la fecha y la hora siempre y cuando sea una
		// transaccion calendarizada
		boolean fechaValida = true;
		if (parametros.getTransaccionForm().getCalendarizada()) {
			try {
				GregorianCalendar fecha = parametros.getTransaccionForm().getCalendarizacion().getFechaAsCalendar();
				if (fecha.getTime().before(new GregorianCalendar().getTime())) {
					parametros.getMessages().add("calendarizacion",new ActionMessage("errors.calendarizacion.fechaanterior"));
					fechaValida = false;
				}
			} catch (Exception e) {
				parametros.getMessages().add("calendarizacion", new ActionMessage("errors.calendarizacion.fechainvalida"));
				fechaValida = false;
			}
		}
		return fechaValida;
	}

	// Guarda la transaccion y dependiendo de wait entonces espera o no que
	// termine de guardar
	private void saveTransaccion(ParametrosPlanilla parametros,Usuario usuario, boolean wait) throws Exception {
		List logs = new ArrayList();
		try {
			List lista;
			boolean esMultiple;
			LineaTransaccion lineaNueva;
			Transaccion transaccionSession;
			transaccionSession = parametros.getTransaccionFormSession().getTransaccion();
			logs.add(getClass().getName() + ".saveTransaccion() wait=" + wait);
			logs.add("----Armamos la transaccion");
			parametros.getTransaccionFormSession().setTransaccionFromForm();
			if (parametros.getTransaccionFormSession().getCalendarizada()) {
				logs.add("Seteamos la fecha de calendarizacion");
				transaccionSession.setFechaProgramacionAutomatica(parametros.getTransaccionFormSession().getCalendarizacion().getFechaAsDate().getTime());
			}

			{
				logs.add("Verificamos los campos requeridos que vienen en null, estos campos son null porque la transaccion no ha sido aplicada");
				if (transaccionSession.getReferencia() == null)
					transaccionSession.setReferencia("");

				if (transaccionSession.getDescripcion() == null)
					transaccionSession.setDescripcion("");

				transaccionSession.setUsuario(usuario);
				transaccionSession.setCliente(usuario.getCliente());

				logs.add("El estado inicial de la transaccion es pendiente de autorizacion");
				if (parametros.getTransaccionFormSession().getTipoTransaccion().equals(TipoTransaccion.VERIFICACION_CUENTAS)) {
					transaccionSession.setEstado(EstadoTransaccion.ESTADO_PENDIENTE_APLICACION);
				} else
					transaccionSession.setEstado(EstadoTransaccion.ESTADO_PENDIENTE_AUTORIZACION);

				/**
				 * TODO NUEVA VERSION CH20091109 ESTE CAMBIO ES NECESARIO
				 * IMPLEMENTARLO POR LA INCORPORACION DEL TIPO DE TRANSACCION
				 * PUBLICACION DE DOCUMENTOS SI LA TRANSACCION HA SIDO
				 * PROGRAMADA Y ES DE TIPO PUBLICACION_DOCUMENTOS_AE, ENTONCES
				 * DEBE SER PUESTA COMO PROGRAMADA POR APLICAR PARA SALTARSE LAS
				 * REGLAS DE AUTORIZACION. SINO SE REALIZA ESTE CAMBIO ENTONCES
				 * LA TRANSACCION SIEMPRE QUEDARIA CON ESTADO PENDIENTE DE
				 * AUTORIZACION Y NUNCA SE EJECUTARIA POR EL TRANSACCIONDEAMON.
				 */
				if (parametros.getTransaccionFormSession().getTipoTransaccion().equals(TipoTransaccion.PUBLICACION_DOCUMENTOS_AE)
						&& parametros.getTransaccionFormSession().getCalendarizada())
					transaccionSession.setEstado(EstadoTransaccion.ESTADO_PROGRAMADA_POR_APLICAR);

				logs.add("Actualizamos las fechas");
				parametros.getTransaccionFormSession().setFechaActual(parametros.getTransaccionForm().getFechaFromServer());
				parametros.getTransaccionFormSession().setFechaSistema(parametros.getTransaccionForm().getFechaFromIBS());

				logs.add("Seteamos la fecha de creacion");
				transaccionSession.setFechaCreacion(parametros.getTransaccionForm().getFechaFromServer().getTime().getTime());

				logs.add("Proceso que actualiza el campo memo de una transaccion multiple");
				logs.add("Verificamos si la transacción es multiple: "+ transaccionSession.getTipoTransaccion());
				lista = transaccionSession.getLineaTransaccionList();
				esMultiple = transaccionSession.getTipoTransaccion().getTipoOperacion().equals(TipoOperacion.MULTIPLE);
				for (int i = 0; i < lista.size(); i++) {
					lineaNueva = (LineaTransaccion) lista.get(i);
					if (lineaNueva.getReferencia() == null
							|| transaccionSession.getTipoTransaccion().equals(TipoTransaccion.ADICION_CUENTAS_PREDEFINIDAS)
							|| transaccionSession.getTipoTransaccion().equals(TipoTransaccion.ELIMINAR_CUENTAS_PREDEFINIDAS)) {
						lineaNueva.setReferencia("");
					}

					//Caso especial donde el memo de la transaccion debe ser un objeto cuenta v1
					/*
					 * if (esMultiple) {
					 * if(!transaccionSession.getTipoTransaccion().equals(TipoTransaccion.PUBLICACION_DOCUMENTOS_AE) &&
					 * !transaccionSession.getTipoTransaccion().equals(TipoTransaccion.ANTICIPO_DOCUMENTOS) &&
					 * !transaccionSession.getTipoTransaccion().equals(TipoTransaccion.PRE_PUBLICACION_DOCUMENTOS)){
					 * ObservacionPlanillas observacion = new
					 * ObservacionPlanillas(); observacion.setObservacion(new
					 * String(lineaNueva.getMemoAsBytes()));
					 * observacion.setAlias(lineaNueva.getCuenta().getAlias());
					 * lineaNueva.setMemoAsBytes(observacion.toBytes()); } }
					 */
					// v2
					// cpalacios; diciembre2015; Identificacion Depositos.
					if (esMultiple) {
						if (!transaccionSession.getTipoTransaccion().equals(TipoTransaccion.PUBLICACION_DOCUMENTOS_AE)
								&& !transaccionSession.getTipoTransaccion().equals(TipoTransaccion.ANTICIPO_DOCUMENTOS)
								&& !transaccionSession.getTipoTransaccion().equals(TipoTransaccion.PRE_PUBLICACION_DOCUMENTOS)
								&& !transaccionSession.getTipoTransaccion().equals(TipoTransaccion.IDENTIFICACION_DEPOSITOS)) {
							ObservacionPlanillas observacion = new ObservacionPlanillas();
							observacion.setObservacion(new String(lineaNueva.getMemoAsBytes()));
							observacion.setAlias(lineaNueva.getCuenta().getAlias());
							lineaNueva.setMemoAsBytes(observacion.toBytes());
						}
					}
				}
				logs.add("fuera del for() - " + lista.size());
			}
			// Se crea un nuevo campo extensión al encabezado, para aquellas
			// transacciones a las que se les aplicará cobro de impuesto
			if (parametros.getTransaccionFormSession().isCobroImpuesto()) {
				agregarCampoExtImpuesto(transaccionSession);
			}

			logs.add("Cantidad de lineas antes de guardar "+ parametros.getTransaccionFormSession().getTransaccion().getLineaTransaccionList().size());
			// En una transaccion multiple debemos tener almenos 2 lineas de
			// transaccion (cargo y abono)
			// Esta condicion de cumple cuando el usuario utiliza el boton
			// "back" propio del browser
			if (esMultiple && lista.size() < 1) {
				System.out.println("se dio error");
				logs.add("Imposible guardar una transaccion con menos de 2 lineas, al parecer el usuario utilizo el boton 'back' del browser");
				parametros.getErrors().add("transacciones",new ActionError("errors.transaccion.sinlineas"));
			} else {
				wait &= lista.size() < 99;
				TransaccionUtils.save(logs, transaccionSession, wait);
				// Para obligar a sincronizar la transacción
				if (transaccionSession.getTipoTransaccion().equals(TipoTransaccion.DESEMBOLSO_CREDIPOS)
						|| transaccionSession.getTipoTransaccion().equals(TipoTransaccion.DESEMBOLSO_EN_LINEA_LC)) {
					parametros.getTransaccionFormSession().setTransaccion(transaccionSession); // validar porque va esta validacion
				}

				// TODO************NUEVO********************Verificamos si se ha
				// programado para que sea recurrente...
				if (parametros.getTransaccionFormSession().getCalendarizada()
						&& parametros.getTransaccionFormSession().getRecurrente()
						&& parametros.getTransaccionFormSession().getCantTrx() > 0) {
					crearTransaccionesProgramadas(logs, parametros.getTransaccionFormSession(), transaccionSession,wait);
				}
				// TODO**********FIN_NUEVO******************************************
				if (transaccionSession.getTipoTransaccion().equals(TipoTransaccion.PLANILLA_CONFIRMACION_CHEQUES)
						|| transaccionSession.getTipoTransaccion().equals(TipoTransaccion.PUBLICACION_DOCUMENTOS_AE)
						|| transaccionSession.getTipoTransaccion().equals(TipoTransaccion.ANTICIPO_DOCUMENTOS)
						|| transaccionSession.getTipoTransaccion().equals(TipoTransaccion.VERIFICACION_CUENTAS)) {
					while (transaccionSession.getEstado().equals(EstadoTransaccion.ESTADO_GENERANDO)) {
						Utils.sleepQuietly(300);
					}
				}
				// ----Seteamos el estado de la transaccion como enviada
				parametros.getTransaccionFormSession().setEstado(TransaccionGenericoForm.ESTADO_EJECUTADO);
			}
		} finally {
			if (wait) {
				LogHelper.list(logs);
			}
		}
	}

	/**
	 * Ejecuta la creacion de las transacciones programadas
	 * 
	 * @param transaccionGenericoFromSession
	 */
	public void crearTransaccionesProgramadas(List logs, TransaccionGenericoForm transaccionGenericoFromSession,Transaccion transaccionSession, boolean wait) throws Exception {
		if (transaccionGenericoFromSession.getPeriodo() != null) {
			// Semanal
			if (transaccionGenericoFromSession.getPeriodo().trim()
					.equalsIgnoreCase(SEMANAL)) {
				createTransaccionSemanales(logs,
						transaccionGenericoFromSession, transaccionSession,
						wait);
			}
			// Quincenal
			if (transaccionGenericoFromSession.getPeriodo().trim()
					.equalsIgnoreCase(QUINCENAL)) {
				createTransaccionQuincenales(logs,
						transaccionGenericoFromSession, transaccionSession,
						wait);
			}
			// Mensual
			if (transaccionGenericoFromSession.getPeriodo().trim()
					.equalsIgnoreCase(MENSUAL)) {
				createTransaccionMensuales(logs,
						transaccionGenericoFromSession, transaccionSession,
						wait);
			}
			// Trimestral
			if (transaccionGenericoFromSession.getPeriodo().trim()
					.equalsIgnoreCase(TRIMESTRAL)) {
				createTransaccionTrimestrales(logs,
						transaccionGenericoFromSession, transaccionSession,
						wait);
			}
		}
	}

	/**
	 * Permite crear transacciones programadas SEMANALMENTE de forma recurrente,
	 * segun la fecha de inicio y la cantidad de transacciones especificadas,
	 * para la transaccion que se esta procesando en ese momento
	 * 
	 * @param logs
	 * @param transaccionGenericoFromSession
	 * @param transaccionSession
	 * @param wait
	 * @throws Exception
	 */
	public void createTransaccionSemanales(List logs,
			TransaccionGenericoForm transaccionGenericoFromSession,
			Transaccion transaccionSession, boolean wait) throws Exception {
		// Obtengo la fecha de pago registrada
		int diaPago = transaccionGenericoFromSession.getCalendarizacion()
				.getFechaAplicacionDia();
		int mesPago = transaccionGenericoFromSession.getCalendarizacion()
				.getFechaAplicacionMes();
		int anioPago = transaccionGenericoFromSession.getCalendarizacion()
				.getFechaAplicacionAnno();
		int hora = transaccionGenericoFromSession.getCalendarizacion()
				.getFechaAplicacionHora();

		// Obtenemos datos necesarios
		int daysInAMonth = FechaUtils.getNumberDaysInAMonth(anioPago, mesPago);
		int dayOfWeekForADate = FechaUtils.getDayOfWeekForAPaticularDate(
				anioPago, mesPago, diaPago);
		int newDayOfWeekForOtherDate = 0;
		// Creamos la cantidad de transacciones especificadas
		for (int i = 0; i < (transaccionGenericoFromSession.getCantTrx() - 1); i++) {
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
				// Obtenemos la cantidad de dias del mes para tener un parametro
				// mas exacto
				daysInAMonth = FechaUtils.getNumberDaysInAMonth(anioPago,
						mesPago);
			}

			newDayOfWeekForOtherDate = FechaUtils
					.getDayOfWeekForAPaticularDate(anioPago, mesPago, diaPago);
			if (dayOfWeekForADate == newDayOfWeekForOtherDate) {
				// actualizamos la fecha de la programacion
				transaccionGenericoFromSession.getCalendarizacion()
						.setFechaAplicacionDia(diaPago);
				transaccionGenericoFromSession.getCalendarizacion()
						.setFechaAplicacionMes(mesPago);
				transaccionGenericoFromSession.getCalendarizacion()
						.setFechaAplicacionAnno(anioPago);
				transaccionGenericoFromSession.getCalendarizacion()
						.setFechaAplicacionHora(hora);

				transaccionSession
						.setFechaProgramacionAutomatica(transaccionGenericoFromSession
								.getCalendarizacion().getFechaAsDate()
								.getTime());
				transaccionSession
						.setEstado(EstadoTransaccion.ESTADO_PENDIENTE_AUTORIZACION);
				TransaccionUtils.save(logs, transaccionSession, wait);
			} else {
				System.err.println("**:o( Error para la fecha!!!==>" + diaPago
						+ "/" + mesPago + "/" + anioPago);
			}
		}
	}

	/**
	 * Permite crear transacciones programadas QUINCENALMENTE de forma
	 * recurrente, segun la fecha de inicio y la cantidad de transacciones
	 * especificadas, para la transaccion que se esta procesando en ese momento
	 * 
	 * @param logs
	 * @param transaccionGenericoFromSession
	 * @param transaccionSession
	 * @param wait
	 * @throws Exception
	 */
	public void createTransaccionQuincenales(List logs,
			TransaccionGenericoForm transaccionGenericoFromSession,
			Transaccion transaccionSession, boolean wait) throws Exception {
		int diaPago = transaccionGenericoFromSession.getCalendarizacion()
				.getFechaAplicacionDia();
		int mesPago = transaccionGenericoFromSession.getCalendarizacion()
				.getFechaAplicacionMes();
		int anioPago = transaccionGenericoFromSession.getCalendarizacion()
				.getFechaAplicacionAnno();
		int hora = transaccionGenericoFromSession.getCalendarizacion()
				.getFechaAplicacionHora();

		// Obtenemos datos necesarios
		int daysInAMonth = FechaUtils.getNumberDaysInAMonth(anioPago, mesPago);

		// Creamos la cantidad de transacciones especificadas
		for (int i = 0; i < (transaccionGenericoFromSession.getCantTrx() - 1); i++) {
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
				// Obtenemos la cantidad de dias del mes para tener un parametro
				// mas exacto
				daysInAMonth = FechaUtils.getNumberDaysInAMonth(anioPago,
						mesPago);
			}

			// actualizamos la fecha de la programacion
			transaccionGenericoFromSession.getCalendarizacion()
					.setFechaAplicacionDia(diaPago);
			transaccionGenericoFromSession.getCalendarizacion()
					.setFechaAplicacionMes(mesPago);
			transaccionGenericoFromSession.getCalendarizacion()
					.setFechaAplicacionAnno(anioPago);
			transaccionGenericoFromSession.getCalendarizacion()
					.setFechaAplicacionHora(hora);

			transaccionSession
					.setFechaProgramacionAutomatica(transaccionGenericoFromSession
							.getCalendarizacion().getFechaAsDate().getTime());
			transaccionSession
					.setEstado(EstadoTransaccion.ESTADO_PENDIENTE_AUTORIZACION);
			TransaccionUtils.save(logs, transaccionSession, wait);
		}
	}

	/**
	 * Permite crear transacciones programadas MENSUALMENTE de forma recurrente,
	 * segun la fecha de inicio y la cantidad de transacciones especificadas,
	 * para la transaccion que se esta procesando en ese momento
	 * 
	 * @param logs
	 * @param transaccionGenericoFromSession
	 * @param transaccionSession
	 * @param wait
	 * @throws Exception
	 */
	public void createTransaccionMensuales(List logs,
			TransaccionGenericoForm transaccionGenericoFromSession,
			Transaccion transaccionSession, boolean wait) throws Exception {
		int diaPago = transaccionGenericoFromSession.getCalendarizacion()
				.getFechaAplicacionDia();
		int mesPago = transaccionGenericoFromSession.getCalendarizacion()
				.getFechaAplicacionMes();
		int anioPago = transaccionGenericoFromSession.getCalendarizacion()
				.getFechaAplicacionAnno();
		int hora = transaccionGenericoFromSession.getCalendarizacion()
				.getFechaAplicacionHora();

		// Creamos la cantidad de transacciones especificadas
		int daysInAMonth = FechaUtils.getNumberDaysInAMonth(anioPago, mesPago);
		int diaPagoReal = 0;
		for (int i = 0; i < (transaccionGenericoFromSession.getCantTrx() - 1); i++) {
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
			// Obtenemos la cantidad de dias del mes para tener un parametro mas
			// exacto
			daysInAMonth = FechaUtils.getNumberDaysInAMonth(anioPago, mesPago);

			if (diaPagoReal > daysInAMonth) {
				diaPagoReal = daysInAMonth;
			}

			// actualizamos la fecha de la programacion
			transaccionGenericoFromSession.getCalendarizacion()
					.setFechaAplicacionDia(diaPagoReal);
			transaccionGenericoFromSession.getCalendarizacion()
					.setFechaAplicacionMes(mesPago);
			transaccionGenericoFromSession.getCalendarizacion()
					.setFechaAplicacionAnno(anioPago);
			transaccionGenericoFromSession.getCalendarizacion()
					.setFechaAplicacionHora(hora);

			transaccionSession
					.setFechaProgramacionAutomatica(transaccionGenericoFromSession
							.getCalendarizacion().getFechaAsDate().getTime());
			transaccionSession
					.setEstado(EstadoTransaccion.ESTADO_PENDIENTE_AUTORIZACION);
			TransaccionUtils.save(logs, transaccionSession, wait);
		}
	}

	/**
	 * 
	 * @param logs
	 * @param transaccionGenericoFromSession
	 * @param transaccionSession
	 * @param wait
	 * @throws Exception
	 */
	public void createTransaccionTrimestrales(List logs,
			TransaccionGenericoForm transaccionGenericoFromSession,
			Transaccion transaccionSession, boolean wait) throws Exception {
		int diaPago = transaccionGenericoFromSession.getCalendarizacion()
				.getFechaAplicacionDia();
		int mesPago = transaccionGenericoFromSession.getCalendarizacion()
				.getFechaAplicacionMes();
		int anioPago = transaccionGenericoFromSession.getCalendarizacion()
				.getFechaAplicacionAnno();
		int hora = transaccionGenericoFromSession.getCalendarizacion()
				.getFechaAplicacionHora();

		// Creamos la cantidad de transacciones especificadas
		int daysInAMonth = FechaUtils.getNumberDaysInAMonth(anioPago, mesPago);
		int diaPagoReal = 0;
		for (int i = 0; i < (transaccionGenericoFromSession.getCantTrx() - 1); i++) {
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
			// Obtenemos la cantidad de dias del mes para tener un parametro mas
			// exacto
			daysInAMonth = FechaUtils.getNumberDaysInAMonth(anioPago, mesPago);

			if (diaPagoReal > daysInAMonth) {
				diaPagoReal = daysInAMonth;
			}

			// actualizamos la fecha de la programacion
			transaccionGenericoFromSession.getCalendarizacion()
					.setFechaAplicacionDia(diaPagoReal);
			transaccionGenericoFromSession.getCalendarizacion()
					.setFechaAplicacionMes(mesPago);
			transaccionGenericoFromSession.getCalendarizacion()
					.setFechaAplicacionAnno(anioPago);
			transaccionGenericoFromSession.getCalendarizacion()
					.setFechaAplicacionHora(hora);

			transaccionSession
					.setFechaProgramacionAutomatica(transaccionGenericoFromSession
							.getCalendarizacion().getFechaAsDate().getTime());
			transaccionSession
					.setEstado(EstadoTransaccion.ESTADO_PENDIENTE_AUTORIZACION);
			TransaccionUtils.save(logs, transaccionSession, wait);
		}
	}

	/**
	 * Metodo que debe implementarse en todas las transacciones, en general se
	 * encarga de validar algunas condiciones para determinar si el usuario
	 * crear o no la transaccion deseada.
	 * 
	 * @param mapping
	 *            Mappind de struts
	 * @param form
	 *            Form con los datos del request
	 * @param request
	 *            Request del usuario
	 * @param response
	 *            Response al usuario
	 * @param parametros
	 *            Clase de parametros que pasan de un lado para otro de las
	 *            clases de las transacciones, contiene el form del request, el
	 *            de la session, los objetos de errores, mensajes y forwars.
	 * @throws Exception
	 *             Cualquier error que se pueda generar
	 * @return Posibles Forwards
	 *         <ul>
	 *         <li>Forward de error</li>
	 *         <li>Forward de creacion</li>
	 *         </ul>
	 */
	public ActionForward executeInicio(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response,
			ParametrosPlanilla parametros) throws Exception {

		throw new Exception("Falta la implementacion de executeInicio en "
				+ this.getClass().getName());
	}

	/**
	 * Debe ser implementada en todos las transacciones. Se encarga de realizar
	 * la verificacion de los datos ingresados por el usuario.
	 * 
	 * @param mapping
	 *            Mappind de struts
	 * @param form
	 *            Form con los datos del request
	 * @param request
	 *            Request del usuario
	 * @param response
	 *            Response al usuario
	 * @param parametros
	 *            Clase de parametros que pasan de un lado para otro de las
	 *            clases de las transacciones, contiene el form del request, el
	 *            de la session, los objetos de errores, mensajes y forwars.
	 * @throws Exception
	 *             Cualquier error que se pueda generar
	 * @return Posibles Forwards
	 *         <ul>
	 *         <li>Forward de error</li>
	 *         <li>Forward de confirmacion</li>
	 *         </ul>
	 */
	public ActionForward executeConfirmacion(ActionMapping mapping,ActionForm form, HttpServletRequest request,HttpServletResponse response, ParametrosPlanilla parametros)throws Exception {
		throw new Exception( "Falta la implementacion de executeConfirmacion en " + this.getClass().getName());
	}

	/**
	 * No se utiliza porque no se implemento la edicion de transacciones
	 */
	public ActionForward executeCorregir(ActionMapping mapping,
			ActionForm form, HttpServletRequest request,
			HttpServletResponse response, ParametrosPlanilla parametros)
			throws Exception {

		throw new Exception("Falta la implementacion de executeCorregir en "
				+ this.getClass().getName());
	}

	/**
	 * PUEDE ser implementada en las transacciones. Se encarga de aplicar una
	 * transaccion, generalmene todas hacen lo mismo, la palabra aplicar toma
	 * dos significados:
	 * <ul>
	 * <li>Un caso es para aplicar la transaccion directamente sobre el AS400</li>
	 * <li>El segundo es guardar la transaccion, para este caso son las
	 * transacciones que se guardan para que queden pendientes de autorizar</li>
	 * </ul>
	 * 
	 * @param mapping
	 *            Mappind de struts
	 * @param form
	 *            Form con los datos del request
	 * @param request
	 *            Request del usuario
	 * @param response
	 *            Response al usuario
	 * @param parametros
	 *            Clase de parametros que pasan de un lado para otro de las
	 *            clases de las transacciones, contiene el form del request, el
	 *            de la session, los objetos de errores, mensajes y forwars.
	 * @throws Exception
	 *             Cualquier error que se pueda generar
	 * @return Posibles Forwards
	 *         <ul>
	 *         <li>Forward de error</li>
	 *         <li>Forward de resultado</li>
	 *         </ul>
	 */
	public ActionForward executeAplicar(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response,
			ParametrosPlanilla parametros) throws Exception {
		// throw new Exception("Falta la implementacion de executeAplicar en " +
		// this.getClass().getName());
		return getForwardResultado(mapping);
	}

	// AFP
	/**
	 * PUEDE ser implementada en las transacciones. Se encarga de guardar algo,
	 * como un archivo
	 * 
	 * @param mapping
	 *            Mappind de struts
	 * @param form
	 *            Form con los datos del request
	 * @param request
	 *            Request del usuario
	 * @param response
	 *            Response al usuario
	 * @param parametros
	 *            Clase de parametros que pasan de un lado para otro de las
	 *            clases de las transacciones, contiene el form del request, el
	 *            de la session, los objetos de errores, mensajes y forwars.
	 * @throws Exception
	 *             Cualquier error que se pueda generar
	 * @return Posibles Forwards
	 *         <ul>
	 *         <li>Forward de error</li>
	 *         <li>Forward de resultado</li>
	 *         </ul>
	 */
	public ActionForward executeGuardar(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response,
			ParametrosPlanilla parametros) throws Exception {

		if (parametros.getTransaccionFormSession() instanceof PagoServicioForm) {
			PagoServicioForm pagoServicioForm = (PagoServicioForm) parametros
					.getTransaccionFormSession();
			if (esColectorAES(pagoServicioForm.getNPE())
					|| Servicios.esColectorDelSurNPE(pagoServicioForm.getNPE())) {
				return this.getForwardAESResultado(mapping);
			}

		}
		return getForwardResultado(mapping);
	}

	public void beforeSaveTransacccion(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response,
			ParametrosPlanilla parametros) throws Exception {

	}

	protected void escribirLog(String metodo, String mensaje) {
		String salida = "["
				+ new Timestamp(System.currentTimeMillis()).toString() + "]["
				+ this.getClass().getName() + "][" + metodo + "][" + mensaje
				+ "]";
		System.out.println(salida);
	}

	// v1
	/*
	 * boolean validaCuadreCargosAbonos(TipoTransaccion tipotransacion){ return
	 * !(tipotransacion.equals(TipoTransaccion.PLANILLA_CONFIRMACION_CHEQUES) ||
	 * tipotransacion.equals(TipoTransaccion.PUBLICACION_DOCUMENTOS_AE) ||
	 * tipotransacion.equals(TipoTransaccion.VERIFICACION_PRESTAMOS) ||
	 * tipotransacion.equals(TipoTransaccion.VERIFICACION_CUENTAS) ||
	 * tipotransacion.equals(TipoTransaccion.PRE_PUBLICACION_DOCUMENTOS) ); }
	 */

	// v2
	// cpalacios; diciembre2015
	boolean validaCuadreCargosAbonos(TipoTransaccion tipotransacion) {
		return !(tipotransacion
				.equals(TipoTransaccion.PLANILLA_CONFIRMACION_CHEQUES)
				|| tipotransacion
						.equals(TipoTransaccion.PUBLICACION_DOCUMENTOS_AE)
				|| tipotransacion
						.equals(TipoTransaccion.VERIFICACION_PRESTAMOS)
				|| tipotransacion.equals(TipoTransaccion.VERIFICACION_CUENTAS)
				|| tipotransacion
						.equals(TipoTransaccion.PRE_PUBLICACION_DOCUMENTOS) || tipotransacion
				.equals(TipoTransaccion.IDENTIFICACION_DEPOSITOS));
	}

	/**
	 * Devuelve el forward de resultado AES Este forward depende del mapping el
	 * cual es diferente para todas las transacciones, por lo tanto si recibimos
	 * el mapping de pago de servicios entonces el forward de "resultado" lo
	 * enviara a la pagina de resultado de un pago de servcicios
	 * 
	 * @param mapping
	 *            Mapping correspondiente a la transaccion que se esta
	 *            utilizando
	 * @return Retorna el forward "aesResultado"
	 */
	public ActionForward getForwardAESResultado(ActionMapping mapping) {
		return mapping.findForward("aesResultado");
	}

	private boolean esColectorAES(String npe) {
		// identifica si es un colector AES
		// CAESS = 1898, CLESA = 2260, EEO = 2253, DEUSEM = 1881
		String colector = npe.substring(0, 4);
		if (colector.equals("1898") || colector.equals("2260")
				|| colector.equals("2253") || colector.equals("1881")) {
			return true;
		}
		return false;
	}
}
