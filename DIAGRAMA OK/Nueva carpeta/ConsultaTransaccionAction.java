package com.fintec.potala.struts.actions.consultas;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

import com.ba.potala.util.Constantes;
import com.ba.servicios.integracion.Servicios;
import com.bancoagricola.frmwrk.common.Helper;
import com.fintec.comunes.LogHelper;
import com.fintec.comunes.Utils;
import com.fintec.hibernate.HibernateMap;
import com.fintec.potala.bac.autorizaciones.AutorizacionesHelper;
import com.fintec.potala.bac.autorizaciones.AutorizacionesUtils;
import com.fintec.potala.bac.autorizaciones.AutorizationException;
import com.fintec.potala.bac.dq.trans.ControlTransaccionFTP;
import com.fintec.potala.bac.dq.trans.TransactionDispacher;
import com.fintec.potala.corporativa.clazzes.Autorizacion;
import com.fintec.potala.corporativa.clazzes.Cliente;
import com.fintec.potala.corporativa.clazzes.Cuenta;
import com.fintec.potala.corporativa.clazzes.EstadisticaTransaccionImpuesto;
import com.fintec.potala.corporativa.clazzes.EstadoTransaccion;
import com.fintec.potala.corporativa.clazzes.LineaTransaccion;
import com.fintec.potala.corporativa.clazzes.MQMensajeDevolucion;
import com.fintec.potala.corporativa.clazzes.TipoOperacion;
import com.fintec.potala.corporativa.clazzes.TipoTransaccion;
import com.fintec.potala.corporativa.clazzes.Transaccion;
import com.fintec.potala.corporativa.clazzes.TransaccionFlags;
import com.fintec.potala.corporativa.clazzes.TransaccionHistorica;
import com.fintec.potala.corporativa.clazzes.TrxEncExt;
import com.fintec.potala.corporativa.clazzes.Usuario;
import com.fintec.potala.corporativa.services.LineaTransaccionList;
import com.fintec.potala.corporativa.services.TransaccionUtils;
import com.fintec.potala.struts.actions.ValidaSessionBaseAction;
import com.fintec.potala.struts.clases.SessionKeys;
import com.fintec.potala.struts.forms.consultas.ConsultaComprobantesForm;
import com.fintec.potala.struts.forms.consultas.ConsultaTransaccionForm;
import com.fintec.potala.struts.forms.consultas.RegistrosGenericoForm;
import com.fintec.potala.web.bac.jdbc.db.BancosACH;
import com.fintec.potala.web.bac.jdbc.db.DatosConvenioDesembLC;
import com.fintec.potala.web.bac.jdbc.db.DatosDesembolsoCrediPOS;
import com.fintec.potala.web.bac.jdbc.db.DatosLineaCreditoDesmbLC;
import com.fintec.potala.web.bac.jdbc.db.DatosUsuariosDB;
import com.fintec.potala.web.bac.jdbc.db.DetalleBancoACH;
import com.fintec.potala.web.bac.jdbc.db.PagoAFPDB;
import com.fintec.potala.web.bac.jdbc.db.SolicitudTransferenciaACHDB;
import com.fintec.potala.web.bac.jdbc.sql.MisFavoritosSQL;
import com.fintec.potala.web.bac.jdbc.sql.OperacionesACHSQL;
import com.fintec.potala.web.bac.jdbc.sql.TransaccionesRechazadasSQL;
import com.fintec.potala.web.clases.LineaDetalle;
import com.fintec.potala.web.clases.ObservacionPlanillas;
import com.fintec.potala.web.clases.solicitudes.SolicitudTransferenciaInternacional;
import com.fintec.potala.web.clases.TipoProductoUtils;
import com.fintec.seguridad.EstadoUsuario;
import com.fintec.seguridad.UsuarioFlag;
import com.fintec.seguridad.validadores.ValidadorSeguridadFuncional;
import com.fintec.potala.struts.actions.TokensUtils;

//Inicio: cpalacios; fechas de corte ISSS y AFP; 16Dic2013;
import com.fintec.potala.struts.actions.ServiciosTransaccionesYConsultas;
//Fin: cpalacios; fechas de corte ISSS y AFP; 16Dic2013;
import com.ibm.math.BigDecimal;

/**
 * Se encarga de realizar todas las consultas relacionadas a las transacciones,
 * por ejemplo consulta de transacciones pendientes de autorizar, consulta de
 * transacciones aplicadas, rechazadas y otras. Dentro de la lista de acciones
 * posibles tenemos:
 * <ul>
 * <li>iniciar: Primera accion que se ejecuta, verifica el tipo de consulta y
 * sea los parametros para que ocurra correctamente</li>
 * <li>consultarTransacciones: Realiza la consulta de los encabezados de las
 * transacciones apartir de los parametros y la opcion antes accesada (Consulta
 * de transacciones eliminadas, aplicadas etc)</li>
 * <li>paginarTransacciones: Cuando la consulta retorna muchos registros se
 * agrupan en paginas para que el usuario navegue.</li>
 * <li>consultarLineas: Permite ver el detalle de una transaccion.</li>
 * <li>detalleLineas: Consulta el detalle de una transaccion, muestra el
 * detalle, y opciones para los comprobantes, autorizar, eliminar, aplicar etc.</li>
 * <li>filtrarLineas: Cuando la transaccion se trata de una transaccion
 * multiple (planilla, pago proovedores) entonces existe la opcion de filtrado
 * de lineas (todas-aplicadas-sin aplicar)</li>
 * <li>filtrarLineasValidas: Cuando la transaccion se trata de una transaccion
 * multiple (planilla, pago proovedores) entonces existe la opcion de filtrado
 * de lineas (todas-validas-no validas)</li>
 * <li>paginarLineas: Cuando las lineas o detalle de una transaccion son muchas
 * entonces hay que paginarlas.</li>
 * <li>eliminar: Verfica las condiciones de la transaccion para eliminarla, por
 * ejemplo verifica que la transaccion no halla sido aplicada</li>
 * <li>aplicar: Envia la transaccion para que se aplique, pero antes verifica
 * el estado de la transaccion, es decir, que este pendiente y autorizada</li>
 * <li>autorizar: Verifica la firma del usuario y autoriza la transacciones,
 * igualmente verifica el estado de la transaccion y la firma del usuario.</li>
 * <li>volver: Realiza la ultima consulta de transacciones realizada por un
 * usuario</li>
 * <li>volveauditoria: Realiza la ultima consulta de transacciones realizada
 * por un usuario auditor</li>
 * <ul>
 * 
 * @author psaenz
 * @version 1.0
 */
public class ConsultaTransaccionAction extends ValidaSessionBaseAction {
	/**
	 * Procesa la peticion especificada del HTTP, y crea la respuesta
	 * correspondiente del HTTP (o remite a otro componente web que la cree).
	 * Devuelve una instancia <code>ActionForward</code> describiendo como el
	 * control debe ser remitido, o <code>null</code> si la respuesta se ha
	 * terminado ya.
	 * 
	 * @param mapping
	 *            El ActionMapping usado para seleccionar esta instancia
	 * @param actionForm
	 *            El opcional ActionForm bean para esta peticion (si existe uno)
	 * @param request
	 *            El HTTP request estamos procesando
	 * @param response
	 *            The HTTP response estamos creando
	 * 
	 * @exception Excepcion
	 *                si la logica del negocio del <code>Action</code> lanza
	 *                una excepcion
	 */
	public ActionForward executeAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
		HttpSession session = request.getSession(false);
		Usuario usuario = getUsuario(session);
		ParametrosConsulta parametros = new ParametrosConsulta(new ActionErrors(), new ActionForward(), new ActionMessages());
		ConsultaTransaccionForm consultaForm = (ConsultaTransaccionForm) form;
		// VALIDA SI LA OPCION INVOCADA ESTA AUTORIZADA PARA EL USUARIO, SEGUN SU PERFIL
		String url = null;
		if (consultaForm.getTipoAccion() == null) {
			url = "acceso.invalido";
		} else if (consultaForm.getTipoAccion().equalsIgnoreCase(ConsultaTransaccionForm.ACCION_TRANSACCION)) {
			if (consultaForm.getEstado().equalsIgnoreCase("pautorizar")) {
				url = "/PotalaCorporativa/consultaTransacciones.do?accion=iniciar&tipoAccion=transaccion&estado=pautorizar";
			} else if (consultaForm.getEstado().equalsIgnoreCase("paplicar")) {
				if (consultaForm.getFiltroUsuario() != null && consultaForm.getFiltroUsuario().equals(ConsultaTransaccionForm.FILTRO_PERFIL)) {
					url = "/PotalaCorporativa/consultaTransacciones.do?accion=iniciar&tipoAccion=transaccion&estado=paplicar&filtroUsuario=perfil";
				} else if (consultaForm.getFiltroUsuario() != null && consultaForm.getFiltroUsuario().equals(ConsultaTransaccionForm.FILTRO_PARTICIPANTE)) {
					url = "/PotalaCorporativa/consultaTransacciones.do?accion=iniciar&tipoAccion=transaccion&estado=paplicar&filtroUsuario=participante";
				} else {
					url = "/PotalaCorporativa/consultaTransacciones.do?accion=iniciar&tipoAccion=transaccion&estado=paplicar&filtroUsuario=todas";
				}
			}
		} else if (consultaForm.getTipoAccion().equalsIgnoreCase(ConsultaTransaccionForm.ACCION_AUDITORIA)) {
			if (consultaForm.getFiltroUsuario() == null) {
				url = "acceso.invalido";
			} else if (consultaForm.getFiltroUsuario().equals(ConsultaTransaccionForm.FILTRO_GENERAL)) {
				url = "/PotalaCorporativa/consultaTransacciones.do?accion=iniciar&tipoAccion=auditoria&filtroUsuario=todas";
			} else if (consultaForm.getFiltroUsuario().equals(ConsultaTransaccionForm.FILTRO_PARTICIPANTE)) {
				url = "/PotalaCorporativa/consultaTransacciones.do?accion=iniciar&tipoAccion=auditoria&filtroUsuario=participante";
			} else if (consultaForm.getFiltroUsuario().equals(ConsultaTransaccionForm.FILTRO_PERFIL)) {
				url = "/PotalaCorporativa/consultaTransacciones.do?accion=iniciar&tipoAccion=auditoria&filtroUsuario=perfil";
			} else if (consultaForm.getFiltroUsuario().equals(ConsultaTransaccionForm.FILTRO_HISTORICO)) {
				url = "/PotalaCorporativa/consultaTransacciones.do?accion=iniciar&tipoAccion=auditoria&filtroUsuario=historico";
			} else {
				url = "acceso.invalido";
			}
		} else if (consultaForm.getTipoAccion().equalsIgnoreCase(ConsultaTransaccionForm.ACCION_CONSULTA)) {
			// para uso interno, no validar
			url = null;
		} else {
			url = "acceso.invalido";
		}
		if (url != null) {
			if (url == "acceso.invalido" || !TransaccionUtils.esUrlValido(usuario.getUser(), url)) {
				parametros.getErrors().add("ingreso.no.autorizado",new ActionMessage("mensaje.simple.individual","Usted no posee autorización para realizar esta operacion."));
				saveErrors(request, parametros.getErrors());
				parametros.setForward(mapping.findForward("error"));
				return parametros.getForward();
			}
		}
		// FINAL DE VALIDACION DE URL <<--
		// Si el filtro 'participante' es seleccionado, se asigna el usuario
		// actual al formulario de consulta
		if (consultaForm.getFiltroUsuario().equals(ConsultaTransaccionForm.FILTRO_PARTICIPANTE)) {
			consultaForm.setUsuario(usuario);
		} else if (consultaForm.getFiltroUsuario().equals(ConsultaTransaccionForm.FILTRO_PERFIL)) {
			Usuario usrTmp = new Usuario();
			usrTmp.setUser("[]");
			consultaForm.setUsuario(usrTmp);
		} else if (consultaForm.getFiltroUsuario().equals(ConsultaTransaccionForm.FILTRO_HISTORICO)) {
			if (consultaForm.getUsuario() == null
					|| consultaForm.getUsuario().getUser() == null
					|| consultaForm.getUsuario().getUser().equals("00")) {
				Usuario usrTmp = new Usuario();
				usrTmp.setUser("[]");
				consultaForm.setUsuario(usrTmp);
			}
			if (consultaForm.getAccion().equals("iniciar")) {
				resetFormHistorico(consultaForm, mapping, request);
			}
		}

		try {
			System.out.println("Accion = " + consultaForm.getAccion());
			// ----Determinamos la accion
			if ("iniciar".equalsIgnoreCase(consultaForm.getAccion())) {
				session.setAttribute("tx", "auditoriaTrx");
				MisFavoritosSQL consultarFavoritos = MisFavoritosSQL.getInstance();
				List misFavoritos = consultarFavoritos.getListaFavoritos(usuario.getCliente(), 0);
				session.setAttribute(SessionKeys.KEY_MIS_FAVORITOS_CUENTA,misFavoritos);
				consultaForm.setSubFiltro(false);
				consultaForm.setPorMonto(false);
				consultaForm.setUsarMonto("1");
				consultaForm.setMontoDesde(0.0);
				consultaForm.setMontoHasta(0.0);
				consultaForm.setTransaccion(new Transaccion());
				consultaForm.setListaLineasTransaccion(new RegistrosGenericoForm());
				consultaForm.setListaTransacciones(new RegistrosGenericoForm());
				if (consultaForm.getFechaHoy() != null
						&& consultaForm.getFechaHoy().equals("1")) {
					consultaForm.setearFechaHoy(mapping, request);
					consultaForm.setFechaHoy(null);
				} else {
					consultaForm.reset(mapping, request);
				}
				consultaForm.setMostrarBotonDetalle(ValidadorSeguridadFuncional.validarAcion(usuario.getPerfil(),Constantes.ACCION_SEGURIDAD_VER_DETALLE));
				consultaForm.setVerDetalle(false);
				if (consultaForm.getNoMostrarFormPendientes()) {
					// ----Si no hay que mostrar el form de consulta pendientes
					// entonces se hace la consulta de una vez
					executeConsultaPendientes(session, consultaForm,parametros, 0);
					if (parametros.getErrors().isEmpty()) {
						saveMessages(request, parametros.getMessages());
						return mapping.findForward("vertransacciones");
					}
				} else {
					return mapping.findForward("vertransacciones");
				}
			}
			// ----Consulta de transacciones
			if ("consultarTransacciones".equalsIgnoreCase(consultaForm.getAccion())) {
				boolean mostrarCuentaFavoritos = false;
				if (consultaForm.getFiltroTipoTransaccion() != null
						&& !consultaForm.getFiltroTipoTransaccion().equals("")) {
					try {
						mostrarCuentaFavoritos = Integer
								.parseInt(consultaForm
										.getFiltroTipoTransaccion().equals(
												"none") ? "0" : consultaForm
										.getFiltroTipoTransaccion()) == TipoTransaccion.TRANSFERENCIA_CUENTAS_TERCEROS
								.getId();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				if (!mostrarCuentaFavoritos) {
					consultaForm.setCuenta(new Cuenta());
				}
				consultaForm.setMostrarCuentaFavorito(mostrarCuentaFavoritos);

				// Para solventar el uso del boton de "ver transacciones
				// rechazadas" IR20151118
				if (!consultaForm.getMostrarBotonDetalle()) {
					consultaForm
							.setMostrarBotonDetalle(ValidadorSeguridadFuncional
									.validarAcion(
											usuario.getPerfil(),
											Constantes.ACCION_SEGURIDAD_VER_DETALLE));
				}

				String favorito = request.getParameter("cuentaFavorito");
				if (favorito != null && favorito.equals("generalFavorito")) {
					consultaForm.setFavorito(true);
				} else {
					consultaForm.setFavorito(false);
				}

				if (consultaForm.getFiltroUsuario().equals(
						ConsultaTransaccionForm.FILTRO_HISTORICO)) {
					// Actualizamos las fechas seleccionadas antes de validar el
					// cambio de parametros
					consultaForm.setFechaDesde(
							consultaForm.getFechaDesdeAnno(), consultaForm
									.getFechaDesdeMes(), consultaForm
									.getFechaDesdeDia());
					consultaForm.setFechaHasta(
							consultaForm.getFechaHastaAnno(), consultaForm
									.getFechaHastaMes(), consultaForm
									.getFechaHastaDia());

					// Validamos antes de la consulta para que no se pierda los
					// valores anteriores.
					boolean cambioParametrosMayores = cambioParametrosMayoresHistorico(consultaForm);

					executeConsulta(session, consultaForm, parametros);

					if ((session.getAttribute("lstUsrsHist") != null && cambioParametrosMayores)
							|| request.getParameter("lstUsrTmp") == null) {
						request.setAttribute("lstUsrsHist", (List) session
								.getAttribute("lstUsrsHist"));
					} else {
						String[] objs = request.getParameter("lstUsrTmp")
								.split("], com.");
						List usrs = new ArrayList();
						for (int i = 0; i < objs.length; i++) {
							int ini = objs[i].indexOf("mUser=") + 6;
							int fin = objs[i].indexOf(",mClave");

							Usuario u = new Usuario();
							u.setUser(objs[i].substring(ini, fin));
							usrs.add(u);
						}

						request.setAttribute("lstUsrsHist", usrs);
					}
					session.removeAttribute("lstUsrsHist");
				} else {
					executeConsulta(session, consultaForm, parametros);
				}

				if (parametros.getErrors().isEmpty()) {
					saveMessages(request, parametros.getMessages());
					return mapping.findForward("vertransacciones");
				}
			}

			// ----Paginacion de transacciones
			if ("paginarTransacciones".equalsIgnoreCase(consultaForm
					.getAccion())) {
				// ----Tomamos la pagina
				int pagina = consultaForm.getCurrentPage();

				// ----Paginamos las transacciones
				consultaForm.getListaTransacciones().setCurrentPage(pagina);

				// ----Mostramos las transacciones
				return mapping.findForward("vertransacciones");
			}

			// ----Consulta de las lineas de una transaccion
			if ("consultarLineas".equalsIgnoreCase(consultaForm.getAccion())) {
				if (consultaForm.getEsAccionAuditoria()) {
					// ----Hacemos la consulta detallada de auditoria
					executeConsultaAuditoria(session, consultaForm, mapping,
							parametros);
					// capalacios; octubre2015;transaccionRechazadas
					try {
						if (consultaForm.getTransaccion().getEstado().equals(
								EstadoTransaccion.ESTADO_RECHAZADA)) {

							System.out
									.println("############## VISUALIZACION TRANSACCION RECHAZADA");
							insertarLogTransRechazadas(consultaForm, usuario);

							try {
								// Comprobar si hay operaciones rechazadas

								String cliente = "";

								Usuario usuario2 = (Usuario) session
										.getAttribute(SessionKeys.KEY_USER);

								if (usuario2 != null) {
									cliente = ""
											+ usuario2.getCliente().getId();
								}

								String vistoPor = null;
								Connection con = HibernateMap.openSession()
										.connection();
								Statement stm = con.createStatement();
								// deberia ser fechaUltimaModificacion lo
								// correcto
								String sql = "select usuario,from_unixtime(fecha_visto/1000) as fecha from log._logTransRechazadasVistas where cliente = "
										+ cliente
										+ " and id_transaccion = "
										+ consultaForm.getTransaccion().getId()
										+ " order by usuario,fecha_visto";

								System.out.println("SQL = >>>" + sql);

								ResultSet rs = stm.executeQuery(sql);

								String usuario_anterior = "";
								String usuario_visto = "";

								while (rs.next()) {
									usuario_visto = rs.getString("usuario");

									if (!usuario_visto
											.equalsIgnoreCase(usuario_anterior)) {
										usuario_anterior = usuario_visto;
										if (vistoPor != null) {
											vistoPor = vistoPor + ", ";
										} else {
											vistoPor = "";
										}

										vistoPor = vistoPor + usuario_visto;
									}

								}

								rs.close();
								stm.close();

								if (vistoPor != null) {
									session.setAttribute("vistoPor", vistoPor);
									System.out
											.println("VISTO POR :" + vistoPor);
								} else {
									session.removeAttribute("vistoPor");
								}

							} catch (Exception e) {
								e.printStackTrace();
							}

						}
					} catch (Exception e) {
						// TODO: handle exception
					}
					saveMessages(request, parametros.getMessages());
					saveErrors(request, parametros.getErrors());
					return mapping.findForward("ver-auditoria");
				} else {
					// ----Hacemos la consulta detallada
					// parametros.setForward(executeConsultaDetallada(session,
					// consultaForm, mapping, parametros));
					parametros.setForward(executeConsultaDetalladaPaginada(
							session, consultaForm, mapping, parametros,
							consultaForm.getListaLineasTransaccion()
									.getLinesXPage(), 0,
							Constantes.FILTRO_TODAS));
					consultaForm.setCurrentPage(1);

					// INICIO: cpalacios; Dic2013/Ene2014; fechas de corte
					// ISSS/AFP;
					// Obtener la ultima linea de transaccion para obtener el
					// MEMO y el valor de la AFP:
					// SE SETEA SI ES PAGAR AFP(UPISSS, CONFIA, CRECER, INPEP) O
					// IPSFA
					int paso = 0;
					if (consultaForm.getTransaccion().getTipoTransaccion()
							.getDescripcion().trim().equals(
									TipoTransaccion.PAGO_AFP.getDescripcion())) {
						// obtener el codigo de tipo de Afp de linea de
						// transaccion
						int tipoCompaniaPago = 0;
						tipoCompaniaPago = ServiciosTransaccionesYConsultas
								.codigoAfpCorte(consultaForm.getTransaccion());

						// periodo de pago de afp de linea de transaccion
						String periodoPagoAfp = "";
						periodoPagoAfp = ServiciosTransaccionesYConsultas
								.periodoPagoAfpCorte(consultaForm
										.getTransaccion());
						System.out
								.println("####PERIODO AFP PAGAR EN TRANSACCION: "
										+ periodoPagoAfp);

						// Pasos
						if (consultaForm.getEstado().equals("pautorizar")) {
							paso = 2;
						} else if (consultaForm.getEstado().equals("paplicar")) {
							paso = 3;
						}

						ServiciosTransaccionesYConsultas
								.fechaCortePago_AFP_consultas(tipoCompaniaPago,
										mapping, parametros, 1, paso,
										periodoPagoAfp);
						if (!parametros.getErrors().isEmpty()) {
							saveErrors(request, parametros.getErrors());
							// parametros.setForward(mapping.findForward("error"));
						}
					}
					if (consultaForm.getTransaccion().getTipoTransaccion()
							.getDescripcion().trim().equals(
									TipoTransaccion.PAGO_ISSS.getDescripcion())
							|| consultaForm
									.getTransaccion()
									.getTipoTransaccion()
									.getDescripcion()
									.trim()
									.equals(
											TipoTransaccion.PAGO_PLANILLA_ISSS_EDUCO
													.getDescripcion())) {
						int pasoBandera = 1;
						int pasoTransaccion = 0;

						// Pasos
						if (consultaForm.getEstado().equals("pautorizar")) {
							pasoTransaccion = 2;
						} else if (consultaForm.getEstado().equals("paplicar")) {
							pasoTransaccion = 3;
						}

						ServiciosTransaccionesYConsultas
								.fechaCortePago_ISSS_consultas(55, mapping,
										parametros, pasoBandera,
										pasoTransaccion);
						if (!parametros.getErrors().isEmpty()) {
							saveErrors(request, parametros.getErrors());
							// parametros.setForward(mapping.findForward("error"));
						}
					}
					// FIN: cpalacios; Dic2013/Ene2014; fechas de corte
					// ISSS/AFP;

					saveMessages(request, parametros.getMessages());
					saveErrors(request, parametros.getErrors());
					return parametros.getForward();
				}
			}

			// ----Consulta de las lineas de una transaccion desde la auditoria
			if ("detalleLineas".equalsIgnoreCase(consultaForm.getAccion())) {
				// ----Hacemos la consulta detallada
				parametros.setForward(executeConsultaDetalladaPaginada(session,
						consultaForm, mapping, parametros, consultaForm
								.getListaLineasTransaccion().getLinesXPage(),
						0, Constantes.FILTRO_TODAS));
				consultaForm.setCurrentPage(1);

				// INICIO: cpalacios; Dic2013/Ene2014; fechas de corte ISSS/AFP;
				// Obtener la ultima linea de transaccion para obtener el MEMO y
				// el valor de la AFP:
				// SE SETEA SI ES PAGAR AFP(UPISSS, CONFIA, CRECER, INPEP) O
				// IPSFA
				int paso = 0;
				if (consultaForm.getTransaccion().getTipoTransaccion()
						.getDescripcion().trim().equals(
								TipoTransaccion.PAGO_AFP.getDescripcion())) {
					// obtener el codigo de tipo de Afp de linea de transaccion
					int tipoCompaniaPago = 0;
					tipoCompaniaPago = ServiciosTransaccionesYConsultas
							.codigoAfpCorte(consultaForm.getTransaccion());
					// periodo de pago de afp de linea de transaccion
					String periodoPagoAfp = "";
					periodoPagoAfp = ServiciosTransaccionesYConsultas
							.periodoPagoAfpCorte(consultaForm.getTransaccion());
					System.out.println("####PERIODO AFP PAGAR EN TRANSACCION: "
							+ periodoPagoAfp);

					// Pasos

					if (consultaForm.getEstado().equals("pautorizar")) {
						paso = 2;
					} else if (consultaForm.getEstado().equals("paplicar")) {
						paso = 3;
					}

					ServiciosTransaccionesYConsultas
							.fechaCortePago_AFP_consultas(tipoCompaniaPago,
									mapping, parametros, 1, paso,
									periodoPagoAfp);

					if (!parametros.getErrors().isEmpty()) {
						saveErrors(request, parametros.getErrors());
						// parametros.setForward(mapping.findForward("error"));
					}
				}
				if (consultaForm.getTransaccion().getTipoTransaccion()
						.getDescripcion().trim().equals(
								TipoTransaccion.PAGO_ISSS.getDescripcion())
						|| consultaForm
								.getTransaccion()
								.getTipoTransaccion()
								.getDescripcion()
								.trim()
								.equals(
										TipoTransaccion.PAGO_PLANILLA_ISSS_EDUCO
												.getDescripcion())) {
					int pasoBandera = 1;
					int pasoTransaccion = 0;

					if (consultaForm.getEstado().equals("pautorizar")) {
						pasoTransaccion = 2;
					} else if (consultaForm.getEstado().equals("paplicar")) {
						pasoTransaccion = 3;
					}

					ServiciosTransaccionesYConsultas
							.fechaCortePago_ISSS_consultas(55, mapping,
									parametros, pasoBandera, pasoTransaccion);
					if (!parametros.getErrors().isEmpty()) {
						saveErrors(request, parametros.getErrors());
						// parametros.setForward(mapping.findForward("error"));
					}
				}
				// FIN: cpalacios; Dic2013/Ene2014; fechas de corte ISSS/AFP;

				saveMessages(request, parametros.getMessages());
				saveErrors(request, parametros.getErrors());
				return parametros.getForward();
			}

			// ----filtrar las lineas de transacciones
			if ("filtrarLineas".equalsIgnoreCase(consultaForm.getAccion())) {
				// ----Filtramos las lineas
				// parametros.setForward(executeFiltrarLineas(session,
				// consultaForm, mapping));
				parametros
						.setForward(executeFiltrarLineasPaginadas(session,
								consultaForm, mapping, parametros, consultaForm
										.getListaLineasTransaccion()
										.getLinesXPage(), 0));
				saveMessages(request, parametros.getMessages());
				saveErrors(request, parametros.getErrors());
				return parametros.getForward();
			}

			// ----filtrar las lineas de transacciones cuando se trata de
			// verificador de cuentas o prestamos
			if ("filtrarLineasValidas".equalsIgnoreCase(consultaForm
					.getAccion())) {
				// ----Filtramos las lineas
				// parametros.setForward(executeFiltrarLineas(session,
				// consultaForm, mapping));
				parametros.setForward(executeFiltrarLineasPaginadasValidas(
						session, consultaForm, mapping, parametros,
						consultaForm.getListaLineasTransaccion()
								.getLinesXPage(), 0));
				saveMessages(request, parametros.getMessages());
				saveErrors(request, parametros.getErrors());
				return parametros.getForward();
			}

			// ----Paginacion de las lineas de una transaccion
			if ("paginarLineas".equalsIgnoreCase(consultaForm.getAccion())) {
				// ----Tomamos la pagina
				int pagina = consultaForm.getCurrentPage();

				// ----Paginamos las lineas de la transaccion
				consultaForm.setCurrentPage(pagina);
				// consultaForm.getListaLineasTransaccion().setCurrentPage(pagina);
				executeFiltrarLineasPaginadas(session, consultaForm, mapping,
						parametros, consultaForm.getListaLineasTransaccion()
								.getLinesXPage(), consultaForm.getIndex());
				// ----Mostramos el detalle de las transacciones
				return mapping.findForward("verdetalle");
			}

			// ----Eliminar la transaccion
			if ("eliminar".equalsIgnoreCase(consultaForm.getAccion())) {
				// ---- Volvemos a cargar la transaccion para asegurar que
				// tenemos lo ultimo de esa transaccion y refrescar cualquier
				// valor que halla cambiado
				Transaccion transaccion = TransaccionUtils.loadTransaccion(
						usuario.getCliente(), consultaForm.getIdTransaccion());
				consultaForm.setTransaccion(transaccion);

				if (transaccion.getEstado().equals(
						EstadoTransaccion.ESTADO_PENDIENTE_APLICACION)
						|| transaccion
								.getEstado()
								.equals(
										EstadoTransaccion.ESTADO_PENDIENTE_AUTORIZACION)
						|| transaccion
								.getEstado()
								.equals(
										EstadoTransaccion.ESTADO_PROGRAMADA_POR_APLICAR)) {
					TransaccionUtils.eliminar(transaccion, usuario);
				} else {
					// cualquier otro estado diferente a pendiente de autorizar
					parametros.getErrors().add(
							"autorizacion",
							new ActionError(
									"mensajes.detalletransaccion.noaplicando"));
				}

				// ----Volvemos hacer la consulta detallada de la misma
				// transaccion
				consultaForm.setIdTransaccion(transaccion.getId());
				parametros.setForward(executeConsultaDetallada(session,
						consultaForm, mapping, parametros));

				saveMessages(request, parametros.getMessages());
				saveErrors(request, parametros.getErrors());

				return parametros.getForward();
			}

			// ----aplicar la transaccion
			if ("aplicar".equalsIgnoreCase(consultaForm.getAccion())) {
				// ---- Volvemos a cargar la transaccion para asegurar que
				// tenemos lo ultimo de esa transaccion y refrescar cualquier
				// valor que halla cambiado
				Transaccion transaccion;

				transaccion = TransaccionUtils.loadTransaccion(usuario
						.getCliente(), consultaForm.getIdTransaccion());

				if (transaccion.getTipoTransaccion().getDescripcion().trim()
						.equals(TipoTransaccion.DATOS_USUARIO.getDescripcion())
						|| transaccion
								.getTipoTransaccion()
								.getDescripcion()
								.trim()
								.equals(
										TipoTransaccion.SOLICITUD_AFILIACION_TRX_AUTOMATICA
												.getDescripcion())) {
					// cpalacios; 20160628; auditoriaUsuarios; se agrega ip y se
					// guarda en base de datos
					String ip = "";
					ip = request.getRemoteAddr() != null ? request
							.getRemoteAddr() : " ";
					System.out.println("ip remota de cambio datos: " + ip);

					transaccion.setIpRemota(ip);

					System.out.println("guardarIpRemota: " + ip);
					// transaccion.setDescripcion(ip);
					transaccion.setMemoAsBytes(Utils.writeObject(ip));

					HibernateMap.update(transaccion);
				}

				consultaForm.setTransaccion(transaccion);

				// ----Validamos el estado y que la transaccion no sea
				// programada
				if (transaccion.getEstado().equals(
						EstadoTransaccion.ESTADO_PENDIENTE_APLICACION)) {
					// INICIO: cpalacios; Dic2013/Ene2014; fechas de corte
					// ISSS/AFP;
					boolean banderaRechazoPago = true;
					if (consultaForm.getTransaccion().getTipoTransaccion()
							.getDescripcion().trim().equals(
									TipoTransaccion.PAGO_AFP.getDescripcion())) {
						// obtener el codigo de tipo de Afp de linea de
						// transaccion
						int tipoCompaniaPago = 0;
						tipoCompaniaPago = ServiciosTransaccionesYConsultas
								.codigoAfpCorte(transaccion);
						// periodo de pago de afp de linea de transaccion
						String periodoPagoAfp = "";
						periodoPagoAfp = ServiciosTransaccionesYConsultas
								.periodoPagoAfpCorte(transaccion);
						System.out
								.println("####PERIODO AFP PAGAR EN TRANSACCION: "
										+ periodoPagoAfp);

						banderaRechazoPago = ServiciosTransaccionesYConsultas
								.fechaCortePago_AFP_consultas(tipoCompaniaPago,
										mapping, parametros, 1, 3,
										periodoPagoAfp);
						if (!parametros.getErrors().isEmpty()) {
							transaccion
									.setEstado(EstadoTransaccion.ESTADO_RECHAZADA);
							HibernateMap.update(transaccion);
							TransaccionUtils
									.pasarTransaccionAHistorica_v2(transaccion);
							saveMessages(request, parametros.getMessages());
							saveErrors(request, parametros.getErrors());
							return mapping.findForward("verdetalle");
						}
					} else if (consultaForm.getTransaccion()
							.getTipoTransaccion().getDescripcion().trim()
							.equals(TipoTransaccion.PAGO_ISSS.getDescripcion())
							|| consultaForm
									.getTransaccion()
									.getTipoTransaccion()
									.getDescripcion()
									.trim()
									.equals(
											TipoTransaccion.PAGO_PLANILLA_ISSS_EDUCO
													.getDescripcion())) {
						int pasoBandera = 1;
						int pasoTransaccion = 3;
						banderaRechazoPago = ServiciosTransaccionesYConsultas
								.fechaCortePago_ISSS_consultas(55, mapping,
										parametros, pasoBandera,
										pasoTransaccion);
						if (!parametros.getErrors().isEmpty()) {
							transaccion
									.setEstado(EstadoTransaccion.ESTADO_RECHAZADA);
							HibernateMap.update(transaccion);
							TransaccionUtils
									.pasarTransaccionAHistorica_v2(transaccion);
							saveMessages(request, parametros.getMessages());
							saveErrors(request, parametros.getErrors());
							// parametros.setForward(mapping.findForward("error"));
							return mapping.findForward("verdetalle");
						}
					}
					// FIN: cpalacios; Dic2013/Ene2014; fechas de corte
					// ISSS/AFP;
					if (validaDisponibilidadServicios(transaccion)) {
						TransactionDispacher.execute(transaccion, usuario);
					} else {
						parametros
								.getErrors()
								.add(
										"autorizacion",
										new ActionError(
												"errors.transaccion.reintentar"));
					}
				} else {
					if (consultaForm.getTransaccion().getEstado().equals(
							EstadoTransaccion.ESTADO_APLICADA)
							|| consultaForm.getTransaccion().getEstado()
									.equals(EstadoTransaccion.ESTADO_RECHAZADA)
							|| consultaForm.getTransaccion().getEstado()
									.equals(EstadoTransaccion.ESTADO_APLICANDO)) {
						// La transaccion ya esta aplicada o esta siendo
						// aplicada
						parametros
								.getErrors()
								.add(
										"autorizacion",
										new ActionError(
												"errors.detalletransaccion.yaprocesada"));
					} else {
						// cualquier otro estado diferente a pendiente de
						// autorizar
						parametros
								.getErrors()
								.add(
										"autorizacion",
										new ActionError(
												"mensajes.detalletransaccion.noaplicando"));
					}
				}

				// ----Volvemos hacer la consulta detallada de la misma
				// transaccion
				consultaForm.setIdTransaccion(transaccion.getId());
				parametros.setForward(executeConsultaDetallada(session,
						consultaForm, mapping, parametros));

				saveMessages(request, parametros.getMessages());
				saveErrors(request, parametros.getErrors());
				return parametros.getForward();
			}

			// ----Autoriza la transaccion por parte del usuario en session
			if ("autorizar".equalsIgnoreCase(consultaForm.getAccion())) {
				// ---- Volvemos a cargar la transaccion para asegurar que
				// tenemos lo ultimo de esa transaccion y refrescar cualquier
				// valor que halla cambiado
				Transaccion transaccion;

				transaccion = TransaccionUtils.loadTransaccion(usuario
						.getCliente(), consultaForm.getIdTransaccion());
				consultaForm.setTransaccion(transaccion);
				executeAutorizar(session, consultaForm, mapping, parametros);
				saveMessages(request, parametros.getMessages());
				saveErrors(request, parametros.getErrors());
				return parametros.getForward();
			}

			// ----Autoriza un grupo de transacciones dada
			if ("autorizarLote".equalsIgnoreCase(consultaForm.getAccion())) {
				executeAutorizarLote(session, consultaForm, mapping, parametros);
				saveMessages(request, parametros.getMessages());
				saveErrors(request, parametros.getErrors());
				executeConsultaPendientes(session, consultaForm, parametros, 0);
				return mapping.findForward("vertransacciones");
			}

			// ----Autoriza un grupo de transacciones dada
			if ("filtrar".equalsIgnoreCase(consultaForm.getAccion())) {
				saveErrors(request, parametros.getErrors());
				executeConsultaPendientes(session, consultaForm, parametros, 1);
				saveMessages(request, parametros.getMessages());
				return mapping.findForward("vertransacciones");
			}

			// ----Autoriza un grupo de transacciones dada
			if ("resetear".equalsIgnoreCase(consultaForm.getAccion())) {
				limpiarParametros(consultaForm);
				executeConsultaPendientes(session, consultaForm, parametros, 2);
				saveMessages(request, parametros.getMessages());
				return mapping.findForward("vertransacciones");
			}

			// ----Autoriza un grupo de transacciones dada
			if ("aplicarLote".equalsIgnoreCase(consultaForm.getAccion())) {
				executeAplicarLote(session, consultaForm, mapping, parametros,
						request);
				saveMessages(request, parametros.getMessages());
				saveErrors(request, parametros.getErrors());
				executeConsultaPendientes(session, consultaForm, parametros, 0);
				return mapping.findForward("vertransacciones");
			}

			// ----Elimina un grupo de transacciones dada
			if ("eliminarLote".equalsIgnoreCase(consultaForm.getAccion())) {
				executeEliminarLote(session, consultaForm, mapping, parametros);
				saveMessages(request, parametros.getMessages());
				saveErrors(request, parametros.getErrors());
				executeConsultaPendientes(session, consultaForm, parametros, 0);
				return mapping.findForward("vertransacciones");
			}

			// ----Volver de la consulta de lineas a la consulta de
			// transacciones
			if ("volver".equalsIgnoreCase(consultaForm.getAccion())) {
				executeVolver(session, consultaForm, mapping, parametros);
				saveMessages(request, parametros.getMessages());
				saveErrors(request, parametros.getErrors());
				// Igualamos la lista por autorizar en 0
				consultaForm.setListaAutorizar(new String[0]);
				return parametros.getForward();
			}

			// ----Volver de la consulta de lineas a la consulta de
			// transacciones
			if ("volverauditoria".equalsIgnoreCase(consultaForm.getAccion())) {
				executePaginar(mapping, consultaForm, request);
				return mapping.findForward("vertransacciones");
			}

			// Ordenar: Toma la ultima consulta realizada y la ordena por el
			// campo elejido
			if ("ordenar".equalsIgnoreCase(consultaForm.getAccion())) {
				return executeOrdenar(mapping, consultaForm, request, response);
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
	 * 
	 * @param session
	 * @param consultaForm
	 * @param mapping
	 * @param parametros
	 * @throws Exception
	 */
	private void executeVolver(HttpSession session,
			ConsultaTransaccionForm consultaForm, ActionMapping mapping,
			ParametrosConsulta parametros) throws Exception {
		if (consultaForm.getEsAccionAuditoria()) {
			// ----Hacemos la consulta detallada de auditoria
			executeConsultaAuditoria(session, consultaForm, mapping, parametros);
			parametros.setForward(mapping.findForward("ver-auditoria"));
		} else {
			if (consultaForm.getNoMostrarFormPendientes()) {
				// Si no tenemos que mostrar el form entonces hacemos la
				// consultamo nuevamente las pendientes de autorizar
				executeConsultaPendientes(session, consultaForm, parametros, 0);
				parametros.setForward(mapping.findForward("vertransacciones"));
			} else {
				executeConsulta(session, consultaForm, parametros);
				parametros.setForward(mapping.findForward("vertransacciones"));
			}
		}
	}

	/**
	 * 
	 * @param session
	 * @param consultaForm
	 * @param parametros
	 * @throws Exception
	 */
	private void executeConsulta(HttpSession session,
			ConsultaTransaccionForm consultaForm, ParametrosConsulta parametros)
			throws Exception {
		List list;
		Transaccion transaccion;
		Usuario usuario = getUsuario(session);
		boolean existeEnSession = false;
		boolean cambioParametros = true;

		// liberamos cualquier memoria no usada
		{
			System.gc();
		}

		// ----Prepara los parametros de consulta de las fechas para traer las
		// transacciones
		GregorianCalendar fechaDesde = new GregorianCalendar();
		fechaDesde.set(Integer.valueOf(consultaForm.getFechaDesdeAnno())
				.intValue(), Integer.valueOf(consultaForm.getFechaDesdeMes())
				.intValue() - 1, Integer.valueOf(
				consultaForm.getFechaDesdeDia()).intValue());

		GregorianCalendar fechaHasta = new GregorianCalendar();
		fechaHasta.set(Integer.valueOf(consultaForm.getFechaHastaAnno())
				.intValue(), Integer.valueOf(consultaForm.getFechaHastaMes())
				.intValue() - 1, Integer.valueOf(
				consultaForm.getFechaHastaDia()).intValue());

		consultaForm.setFechaDesde(consultaForm.getFechaDesdeAnno(),
				consultaForm.getFechaDesdeMes(), consultaForm
						.getFechaDesdeDia());
		consultaForm.setFechaHasta(consultaForm.getFechaHastaAnno(),
				consultaForm.getFechaHastaMes(), consultaForm
						.getFechaHastaDia());

		if (consultaForm.getFiltroUsuario().equals(
				ConsultaTransaccionForm.FILTRO_HISTORICO)) {
			// Para asegurarnos que ha entrado por medio del menu y se debe
			// resetear la consulta.
			if (consultaForm.getListaTransacciones().getRegistros().isEmpty()
					&& consultaForm.getAccion().equals("iniciar")) {
				cambioParametros = true;
				consultaForm.setFiltroTipoTransaccion("");
				consultaForm.getUsuario().setUser("[]");
			} else {
				cambioParametros = cambioParametrosMayoresHistorico(consultaForm);
			}

			if (cambioParametros) {
				consultaForm.setCambioParametros(true);
				consultaForm.setUsarMonto("1");
				consultaForm.cleanup();
				consultaForm.getListaTransacciones().cleanup();
			} else {
				consultaForm.setCambioParametros(false);
			}
		} else {
			/**
			 * Si estos parametros son diferentes de "", entonces no ha habido
			 * un subfiltro *
			 */
			if (!consultaForm.getOldEstado().equals("")
					&& !consultaForm.getOldFechaDesde().equals("")
					&& !consultaForm.getOldFechaHasta().equals("")
					&& !consultaForm.getOldUsuario().equals(""))
				existeEnSession = true;

			/*******************************************************************
			 * Verificamos si ha existido un cambio de parametros principales,
			 * esto para controlar la búsqueda si se hace por el método de
			 * parametros principales, o sino combinado con los otros parametros
			 ******************************************************************/
			if (existeEnSession) {
				if ((consultaForm.getEstado().equals(
						consultaForm.getOldEstado())
						&& consultaForm.getFechaDesde().equals(
								consultaForm.getOldFechaDesde()) && consultaForm
						.getFechaHasta()
						.equals(consultaForm.getOldFechaHasta()))
						&& consultaForm.getUsuario().getUser().equals(
								consultaForm.getOldUsuario())) {
					cambioParametros = false;
					consultaForm.setCambioParametros(false);
				} else {
					cambioParametros = true;
					consultaForm.setCambioParametros(true);
					consultaForm.cleanup();
					consultaForm.getListaTransacciones().cleanup();
				}
			}
		}

		/** Validación de parametros de monto * */
		if (!cambioParametros) {
			if (consultaForm.getUsarMonto().equals("0")
					&& consultaForm.getMontoDesde() == 0
					&& consultaForm.getMontoHasta() == 0) {
				parametros.getMessages().add(
						"consultatransacciones",
						new ActionMessage("mensaje.simple.individual",
								"Los montos deben ser mayores a cero."));
				return;
			}

			if (consultaForm.getUsarMonto().equals("0")
					&& (consultaForm.getMontoDesde() > consultaForm
							.getMontoHasta())) {
				parametros
						.getMessages()
						.add(
								"consultatransacciones",
								new ActionMessage("mensaje.simple.individual",
										"El monto desde debe ser mayor al monto hasta."));
				return;
			}
		}

		// ----Consultamos la transaccion
		if (ConsultaTransaccionForm.ACCION_AUDITORIA.equals(consultaForm
				.getTipoAccion())) {
			consultaForm.getUsuario().setCliente(usuario.getCliente());

			if (consultaForm.isMostrarCuentaFavorito()) {
				Cuenta cuenta = new Cuenta();
				if (consultaForm.isFavorito()) {
					if (consultaForm.getCuentaAsStringFav() != null) {
						cuenta.setNumero(new BigDecimal(consultaForm
								.getCuentaAsStringFav().trim()).toString());
					}
					consultaForm.setCuentaAsString("");
				} else {
					if (consultaForm.getCuentaAsString() != null) {
						cuenta.setNumero(consultaForm.getCuentaAsString()
								.trim());
					}
					consultaForm.setCuentaAsStringFav("");
				}
				consultaForm.setCuenta(cuenta);
			}

			if (consultaForm.getFiltroUsuario().equals(
					ConsultaTransaccionForm.FILTRO_PERFIL)) {
				String opcionesPerfil = TransaccionUtils
						.listarTransaccionesAccesibles(usuario.getUser());
				System.out
						.println("ConsultaTransaccionAction.executeConsulta() Opciones de perfil accesibles: "
								+ opcionesPerfil
								+ ", usuario: "
								+ usuario.getUser());
				if (opcionesPerfil.equals("()")) {
					// no tiene acceso a ninguna opcion
					list = new ArrayList();
				} else {
					if (cambioParametros) {
						System.out
								.println("Ha existido un cambio de parametros superiores, realizamos la búsqueda por parametro superior...");
						list = TransaccionUtils.retrieveTransaccionesUsuario(
								consultaForm.getUsuario(), consultaForm
										.getEstadoTransaccion(), fechaDesde
										.getTime(), fechaHasta.getTime(),
								opcionesPerfil, consultaForm.getCuenta());
					} else {
						System.out
								.println("No ha existido cambio de parametros superiores, filtramos busqueda combinada");
						if (!consultaForm.getSubFiltro()
								&& ((consultaForm.getFiltroTipoTransaccion()
										.equals(null)
										|| consultaForm
												.getFiltroTipoTransaccion()
												.equals("null")
										|| consultaForm
												.getFiltroTipoTransaccion()
												.equals("none") || consultaForm
										.getFiltroTipoTransaccion().equals("")) && (consultaForm
										.getUsarMonto().equals("") || consultaForm
										.getUsarMonto().equals("1"))))
							list = TransaccionUtils
									.retrieveTransaccionesUsuario(consultaForm
											.getUsuario(), consultaForm
											.getEstadoTransaccion(), fechaDesde
											.getTime(), fechaHasta.getTime(),
											opcionesPerfil, consultaForm
													.getCuenta());
						else
							list = TransaccionUtils
									.retrieveTransaccionesUsuario(
											consultaForm.getUsuario(),
											consultaForm.getEstadoTransaccion(),
											fechaDesde.getTime(),
											fechaHasta.getTime(),
											consultaForm
													.getFiltroTipoTransaccion(),
											consultaForm.getUsarMonto(),
											consultaForm.getMontoDesde(),
											consultaForm.getMontoHasta(),
											opcionesPerfil, consultaForm
													.getCuenta());
					}
				}
			} else {

				if (consultaForm.getFiltroUsuario().equals(
						ConsultaTransaccionForm.FILTRO_HISTORICO)) {
					System.out
							.println("ConsultaTransaccionAction.executeConsulta() Opcion Historico Transacciones");
					list = TransaccionUtils.retrieveTransaccionesHistorico(
							usuario, consultaForm, fechaDesde.getTime(),
							fechaHasta.getTime());
				} else {
					if (cambioParametros) {
						System.out
								.println("Ha existido un cambio de parametros superiores, realizamos la búsqueda por parametro superior...");
						list = TransaccionUtils.retrieveTransaccionesUsuario(
								consultaForm.getUsuario(), consultaForm
										.getEstadoTransaccion(), fechaDesde
										.getTime(), fechaHasta.getTime(),
								consultaForm.getTipoTransaccion(), consultaForm
										.getCuenta());
					} else {
						System.out
								.println("No ha existido cambio de parametros superiores, filtramos busqueda combinada");
						if (!consultaForm.getSubFiltro()
								&& ((consultaForm.getFiltroTipoTransaccion()
										.equals(null)
										|| consultaForm
												.getFiltroTipoTransaccion()
												.equals("null")
										|| consultaForm
												.getFiltroTipoTransaccion()
												.equals("none") || consultaForm
										.getFiltroTipoTransaccion().equals("")) && (consultaForm
										.getUsarMonto().equals("") || consultaForm
										.getUsarMonto().equals("1"))))
							list = TransaccionUtils
									.retrieveTransaccionesUsuario(consultaForm
											.getUsuario(), consultaForm
											.getEstadoTransaccion(), fechaDesde
											.getTime(), fechaHasta.getTime(),
											consultaForm.getTipoTransaccion(),
											consultaForm.getCuenta());
						else
							list = TransaccionUtils
									.retrieveTransaccionesUsuario(
											consultaForm.getUsuario(),
											consultaForm.getEstadoTransaccion(),
											fechaDesde.getTime(),
											fechaHasta.getTime(),
											consultaForm
													.getFiltroTipoTransaccion(),
											consultaForm.getUsarMonto(),
											consultaForm.getMontoDesde(),
											consultaForm.getMontoHasta(),
											consultaForm.getCuenta());
					}
				}
			}
		} else {
			list = TransaccionUtils.retrieveTransacciones(usuario.getCliente(),
					consultaForm.getEstadoTransaccion(), fechaDesde.getTime(),
					fechaHasta.getTime());

			// Si estamos trayendo las pendientes de aplicar entonces tambien
			// traemos las programadas pendientes de aplicar
			if (ConsultaTransaccionForm.ACCION_TRANSACCION.equals(consultaForm
					.getTipoAccion())) {
				if (consultaForm.getEstadoTransaccion().equals(
						EstadoTransaccion.ESTADO_PENDIENTE_APLICACION)) {
					list.addAll(TransaccionUtils.retrieveTransacciones(usuario
							.getCliente(),
							EstadoTransaccion.ESTADO_PROGRAMADA_POR_APLICAR));
					// Elimina transacciones que no pertenecen al usuario
					// Se mantiene mecanismo de consulta para evitar duplicidad
					// en la carga de transacciones
					if (consultaForm.getFiltroUsuario().equals(
							ConsultaTransaccionForm.FILTRO_PARTICIPANTE)) {
						Transaccion trxTmp = null;
						Autorizacion autTmp = null;
						Iterator ite = list.iterator();
						while (ite != null && ite.hasNext()) {
							trxTmp = (Transaccion) ite.next();
							if (!TransaccionUtils.usuarioApareceEnTransaccion(
									trxTmp, usuario)) {
								ite.remove();
							}
						}
					}
					// Finaliza eliminacion de transacciones que no pertenecen
					// al usuario
				}
			}

			// Si es consulta de pendientes de autoriza, sacamos las
			// transacciones que el usuario no puede autorizar.
			if (consultaForm.getEstadoTransaccion().equals(
					EstadoTransaccion.ESTADO_PENDIENTE_AUTORIZACION)) {
				if (consultaForm.getAccion().equals(
						ConsultaTransaccionForm.ACCION_TRANSACCION)) {
					Map map;

					try {
						map = getCuentaReglasMap(session);
						for (int i = list.size() - 1; i >= 0; i--) {
							transaccion = (Transaccion) list.get(i);
							if (!AutorizacionesUtils
									.usuarioPuedeAutorizarTransaccion(
											transaccion, usuario, map)) {
								list.remove(i);
							}
						}
					} catch (Exception e) {
						map = getCuentaReglasMap(session, true);
						for (int i = list.size() - 1; i >= 0; i--) {
							transaccion = (Transaccion) list.get(i);
							if (!AutorizacionesUtils
									.usuarioPuedeAutorizarTransaccion(
											transaccion, usuario, map)) {
								list.remove(i);
							}
						}
					}
				}
			}
		}

		consultaForm.getListaTransacciones().setRegistros(list);

		/**
		 * Iteramos las transacciones y las metemos en un hashmap. Para llenar
		 * el combo tipo de transaccion *
		 */
		SortedMap mapaTransacciones = new TreeMap();
		SortedMap mapaUsuarios = new TreeMap();
		Iterator iter = list.iterator();
		TransaccionHistorica transaccionHistoricaLista = null;
		Transaccion transaccionLista = null;
		Object objetoListado = null;

		while (iter.hasNext()) {
			objetoListado = iter.next();
			if (objetoListado instanceof Transaccion) {
				transaccionLista = (Transaccion) objetoListado;
				mapaTransacciones.put(String.valueOf(transaccionLista
						.getTipoTransaccion().getId()), transaccionLista
						.getTipoTransaccion().getDescripcion());
				mapaUsuarios.put(transaccionLista.getUsuario(),
						transaccionLista.getUsuario());
			}
			if (objetoListado instanceof TransaccionHistorica) {
				transaccionHistoricaLista = (TransaccionHistorica) objetoListado;
				mapaTransacciones.put(String.valueOf(transaccionHistoricaLista
						.getTipoTransaccion().getId()),
						transaccionHistoricaLista.getTipoTransaccion()
								.getDescripcion());
				mapaUsuarios.put(transaccionHistoricaLista.getUsuario(),
						transaccionHistoricaLista.getUsuario());
			}
		}

		SortedMap mapaOrdenadoTrx = sortByValue(mapaTransacciones);
		SortedMap mapaOrdenadoUsr = sortByValue(mapaUsuarios);

		Iterator it = mapaOrdenadoUsr.entrySet().iterator();
		List lstUsrs = new ArrayList();
		while (it.hasNext()) {
			Map.Entry obj = (Map.Entry) it.next();
			lstUsrs.add(obj.getValue());
		}
		if (lstUsrs.size() > 0) {
			session.setAttribute("lstUsrsHist", lstUsrs);
		}

		consultaForm.setOldEstado(consultaForm.getEstado());
		consultaForm.setOldFechaDesde(consultaForm.getFechaDesde());
		consultaForm.setOldFechaHasta(consultaForm.getFechaHasta());
		consultaForm.setOldUsuario(consultaForm.getUsuario().getUser());

		if (cambioParametros) {
			System.out.println("Cambio de parametros...");
			session.setAttribute("mapaTransaccion", mapaTransacciones);
			consultaForm.setTiposTransaccion(mapaOrdenadoTrx);
			consultaForm.setPorMonto(false);
			consultaForm.setUsarMonto("1");
			consultaForm.setMontoDesde(0.0);
			consultaForm.setMontoHasta(0.0);
		} else {
			System.out.println("No Cambio de parametros...");
			if (consultaForm.getFiltroUsuario().equals(
					ConsultaTransaccionForm.FILTRO_HISTORICO)) {
				if (!consultaForm.getOldUsuario().equals(
						consultaForm.getUsuario().getUser())) {
					session.setAttribute("mapaTransaccion", mapaTransacciones);
					consultaForm.setTiposTransaccion(mapaOrdenadoTrx);
				} else {
					session.setAttribute("mapaTransaccion", consultaForm
							.getTiposTransaccion());
					consultaForm.setTiposTransaccion(consultaForm
							.getTiposTransaccion());
				}
			} else {
				session.setAttribute("mapaTransaccion", consultaForm
						.getTiposTransaccion());
				consultaForm.setTiposTransaccion(consultaForm
						.getTiposTransaccion());
			}
		}

		// ---- Verificamos si hubo error
		if (parametros.getErrors().isEmpty()) {
			// ---- Verificamos si tiene o no registros
			if (consultaForm.getListaTransacciones().getRegistros().size() == 0) {
				// consultaForm.setSubFiltro(false);
				// ---- El estado es null cuando la consulta es estado="todos"
				if (consultaForm.getEstadoTransaccion() == null) {
					parametros.getMessages().add("consultatransacciones",
							new ActionMessage("errors.consulta.norows"));
				} else {
					if (consultaForm.getEstadoTransaccion().equals(
							EstadoTransaccion.ESTADO_PENDIENTE_AUTORIZACION)) {
						parametros.getMessages().add(
								"consultatransacciones",
								new ActionMessage("mensaje.simple.individual",
										"No hay transacciones para firmar!"));
					} else if (consultaForm.getEstadoTransaccion().equals(
							EstadoTransaccion.ESTADO_PENDIENTE_APLICACION)) {
						parametros.getMessages().add(
								"consultatransacciones",
								new ActionMessage("mensaje.simple.individual",
										"No hay transacciones para aplicar!"));
					} else {
						parametros.getMessages().add("consultatransacciones",
								new ActionMessage("errors.consulta.norows"));
					}
				}
			} else {
				consultaForm.getListaTransacciones().setCurrentPage(1);
				consultaForm.setSubFiltro(true);
			}
		}
	}

	/**
	 * Se encarga de ejecutar la consulta de transacciones pendietes de
	 * autorizar o aplicar, la diferencia con las otras consultas es que no se
	 * hace por rango de fechas indicador = 0, pendientes de firmar/aplicar sin
	 * filtro = 1, pendientes de firmar/aplicar con filtro = 2, pendientes de
	 * firmar/aplicar reset
	 * 
	 * @param session
	 * @param consultaForm
	 * @throws Exception
	 */
	private void executeConsultaPendientes(HttpSession session,
			ConsultaTransaccionForm consultaForm,
			ParametrosConsulta parametros, int indicador) throws Exception {

		System.out.println("Valores form ...");
		System.out.println("Valores secuencial ..."
				+ consultaForm.getFiltroIdTransaccion());
		System.out.println("Valores tipotran ..."
				+ consultaForm.getFiltroTipoTransaccion());
		System.out.println("Valores cliente ..."
				+ consultaForm.getFiltroClienteTransaccion());
		System.out.println("Valores cuenta carga ..."
				+ consultaForm.getFiltroCuentaTransaccion());
		System.out.println("Valores usuario ..."
				+ consultaForm.getFiltroUsuarioTransaccion());
		System.out.println("Valores monto desde ..."
				+ consultaForm.getMontoDesde());
		System.out.println("Valores monto hasta ..."
				+ consultaForm.getMontoHasta());
		System.out.println("accion es  ..." + consultaForm.getAccion());

		Transaccion transaccion;
		Usuario usuario = getUsuario(session);

		List list = new ArrayList();
		// Si estamos trayendo las pendientes de aplicar entonces tambien
		// traemos las pendientes de aplicar y que estan programadas
		if (consultaForm.getEstadoTransaccion().equals(
				EstadoTransaccion.ESTADO_PENDIENTE_APLICACION)) {
			// Se mantiene mecanismo de consulta para evitar duplicidad en la
			// carga de transacciones
			if (consultaForm.getFiltroUsuario().equals(
					ConsultaTransaccionForm.FILTRO_PARTICIPANTE)) {
				if (indicador == 1
						|| (indicador == 0 && !consultaForm.getAccion().equals(
								"iniciar"))) {
					// no hay filtro
					list = TransaccionUtils.retrieveTransacciones(usuario
							.getCliente(), consultaForm.getEstadoTransaccion(),
							consultaForm);
					list.addAll(TransaccionUtils.retrieveTransacciones(usuario
							.getCliente(),
							EstadoTransaccion.ESTADO_PROGRAMADA_POR_APLICAR,
							consultaForm));
				} else {
					list = TransaccionUtils.retrieveTransacciones(usuario
							.getCliente(), consultaForm.getEstadoTransaccion());
					list.addAll(TransaccionUtils.retrieveTransacciones(usuario
							.getCliente(),
							EstadoTransaccion.ESTADO_PROGRAMADA_POR_APLICAR));
				}
				// Elimina transacciones que no pertenecen al usuario
				Transaccion trxTmp = null;
				Autorizacion autTmp = null;
				Iterator ite = list.iterator();
				while (ite != null && ite.hasNext()) {
					trxTmp = (Transaccion) ite.next();
					if (!TransaccionUtils.usuarioApareceEnTransaccion(trxTmp,
							usuario)) {
						ite.remove();
					}
				}
				// Finaliza eliminacion de transacciones que no pertenecen al
				// usuario
			} else if (consultaForm.getFiltroUsuario().equals(
					ConsultaTransaccionForm.FILTRO_PERFIL)) {
				String opcionesPerfil = TransaccionUtils
						.listarTransaccionesAccesibles(usuario.getUser());
				System.out
						.println("ConsultaTransaccionAction.executeConsultaPendientes() Opciones de perfil accesibles: "
								+ opcionesPerfil
								+ ", usuario: "
								+ usuario.getUser());
				if (opcionesPerfil.equals("()")) {
					// no tiene acceso a ninguna opcion
					list = new ArrayList();
				} else {
					if (indicador == 1
							|| (indicador == 0 && !consultaForm.getAccion()
									.equals("iniciar"))) {
						// no hay filtro
						list = TransaccionUtils
								.retrieveTransaccionesFromPerfil(usuario
										.getCliente(), consultaForm
										.getEstadoTransaccion(), consultaForm,
										opcionesPerfil);
						list
								.addAll(TransaccionUtils
										.retrieveTransaccionesFromPerfil(
												usuario.getCliente(),
												EstadoTransaccion.ESTADO_PROGRAMADA_POR_APLICAR,
												consultaForm, opcionesPerfil));
					} else {
						list = TransaccionUtils
								.retrieveTransaccionesFromPerfil(usuario
										.getCliente(), consultaForm
										.getEstadoTransaccion(), opcionesPerfil);
						list
								.addAll(TransaccionUtils
										.retrieveTransaccionesFromPerfil(
												usuario.getCliente(),
												EstadoTransaccion.ESTADO_PROGRAMADA_POR_APLICAR,
												opcionesPerfil));
					}

				}
			} else {
				if (indicador == 1
						|| (indicador == 0 && !consultaForm.getAccion().equals(
								"iniciar"))) {
					// no hay filtro
					list = TransaccionUtils.retrieveTransacciones(usuario
							.getCliente(), consultaForm.getEstadoTransaccion(),
							consultaForm);
					list.addAll(TransaccionUtils.retrieveTransacciones(usuario
							.getCliente(),
							EstadoTransaccion.ESTADO_PROGRAMADA_POR_APLICAR,
							consultaForm));
				} else {
					// no hay filtro
					list = TransaccionUtils.retrieveTransacciones(usuario
							.getCliente(), consultaForm.getEstadoTransaccion());
					list.addAll(TransaccionUtils.retrieveTransacciones(usuario
							.getCliente(),
							EstadoTransaccion.ESTADO_PROGRAMADA_POR_APLICAR));
				}

			}
			/** VALIDACION PARA MOSTRAR MENSAJE LUEGO DE AUTORIZADA * */
			try {
				if (TransaccionUtils.guardarExtensionTransaccion(0, 0, false)
						&& list.size() > 0) {
					parametros
							.getMessages()
							.add(
									"autorizacion",
									new ActionMessage(
											"mensajes.transaccionprocesada.diahabilsiguiente.creacion"));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}// fin pendiente aplicacion

		// filtra las transacciones que son para autorizar al cliente
		boolean esUsuarioUnico = TransaccionUtils.usuarioUnico(usuario);
		boolean esTransaccionToken = false;
		boolean esTransaccionACH = false;
		if (consultaForm.getEstadoTransaccion().equals(
				EstadoTransaccion.ESTADO_PENDIENTE_AUTORIZACION)) {
			if (indicador == 1
					|| (indicador == 0 && !consultaForm.getAccion().equals(
							"iniciar")))
				list = TransaccionUtils.retrieveTransacciones(usuario
						.getCliente(), consultaForm.getEstadoTransaccion(),
						consultaForm);
			else
				list = TransaccionUtils.retrieveTransacciones(usuario
						.getCliente(), consultaForm.getEstadoTransaccion());

			Map map = getCuentaReglasMap(session);
			{
				List keys = new ArrayList(map.keySet());
				for (int i = 0; i < keys.size(); i++) {
					System.out.println("map.get(" + keys.get(i) + ")="
							+ map.get(keys.get(i)));
				}
			}

			for (int i = list.size() - 1; i >= 0; i--) {
				transaccion = (Transaccion) list.get(i);
				try {
					if (!AutorizacionesUtils.usuarioPuedeAutorizarTransaccion(
							transaccion, usuario, map)) {
						list.remove(i);
					}
					/**
					 * Adicionamos validacion para decidir si mostramos campo
					 * token o no *
					 */
					else {
						if (!esTransaccionToken
								&& esUsuarioUnico
								&& TransaccionUtils
										.esTransaccionPorToken(transaccion
												.getTipoTransaccion())) {
							esTransaccionToken = true;
						}
						if (!esTransaccionACH
								&& TransaccionUtils
										.esTransaccionACHPorToken(transaccion
												.getTipoTransaccion().getId())) {
							esTransaccionACH = true;
						}
					}
					/**
					 * Si la variable esTransaccionToken es true mostramos campo
					 * de lo contrario no *
					 */
					if (esTransaccionToken) {
						consultaForm.setMostrarCampoToken(true);
					}
					/**
					 * Si la variable esTransaccionACH es true mostramos campo
					 * token *
					 */
					else if (esTransaccionACH) {
						consultaForm.setTransaccionACH(true);
					} else {
						consultaForm.setTransaccionACH(false);
					}
				} catch (AutorizationException e) {
					list.remove(i);
					parametros.getMessages().add(
							"consultatransacciones",
							new ActionMessage("mensaje.simple.individual",
									"Transaccion: "
											+ transaccion.getSecuencial()
											+ e.getMessage()));
				}
			}
		}
		Collections.sort(list);
		consultaForm.getListaTransacciones().setRegistros(list);

		// ---- Verificamos si tiene o no registros
		if (consultaForm.getListaTransacciones().getRegistros().size() == 0) {
			if (consultaForm.getEstadoTransaccion().equals(
					EstadoTransaccion.ESTADO_PENDIENTE_APLICACION)) {
				parametros.getMessages().add(
						"consultatransacciones",
						new ActionMessage("mensaje.simple.individual",
								"No tiene transacciones para aplicar!"));
			} else {
				if (!usuario.tieneClaveAutorizacion()) {
					parametros
							.getMessages()
							.add(
									"consultatransacciones",
									new ActionMessage(
											"mensaje.simple.individual",
											"Estimado usuario: Aun no ha definido su clave de autorizacion. "
													+ "Para autorizar sus transacciones es necesario que defina su contraseña "
													+ "ingresando al menú: Administración -> Cambio de Autorización."));
				} else {
					parametros.getMessages().add(
							"consultatransacciones",
							new ActionMessage("mensaje.simple.individual",
									"No tiene transacciones para firmar!"));
				}
			}
		} else {
			/**
			 * Iteramos las transacciones y las metemos en un hashmap. Para
			 * llenar el combo tipo de transaccion, id de transaccion *
			 */
			SortedMap mapaTransacciones = new TreeMap();
			SortedMap mapaIdTransacciones = new TreeMap();
			SortedMap mapaIdClientes = new TreeMap();
			SortedMap mapaCuentasCargadas = new TreeMap();
			SortedMap mapaUsuariosTransaccion = new TreeMap();
			List cliente = new ArrayList();
			Cuenta cuenta = null;

			Iterator iter = list.iterator();
			TransaccionHistorica transaccionHistoricaLista = null;
			Transaccion transaccionLista = null;
			Object objetoListado = null;

			String cuentaAsString = "";
			while (iter.hasNext()) {
				objetoListado = iter.next();
				if (objetoListado instanceof Transaccion) {
					transaccionLista = (Transaccion) objetoListado;
					/** tipos de transaccion * */
					mapaTransacciones.put(String.valueOf(transaccionLista
							.getTipoTransaccion().getId()), transaccionLista
							.getTipoTransaccion().getDescripcion());
					/** secuencial de transacciones * */
					mapaIdTransacciones.put(String.valueOf(transaccionLista
							.getSecuencial()), String.valueOf(transaccionLista
							.getSecuencial()));
					/** cuentas cargadas * */
					cuentaAsString = ControlTransaccionFTP.obtenerCuentaCargo(
							transaccionLista.getId(), false);
					if (cuentaAsString != null && cuentaAsString.length() > 0) {
						mapaCuentasCargadas.put(cuentaAsString, cuentaAsString);
					}

					/** Clientes relacionados * */
					cliente = HibernateMap.queryByString(Cuenta.class,
							"where v.numero = '" + cuentaAsString + "'");
					if (cliente != null && cliente.size() > 0) {
						cuenta = (Cuenta) cliente.get(0);
						mapaIdClientes.put(String.valueOf(cuenta.getCliente()
								.getId()), cuenta.getCliente().getNombre());
					}

					/** Usuarios de creacion * */
					mapaUsuariosTransaccion.put(transaccionLista.getUsuario()
							.getUser(), transaccionLista.getUsuario()
							.getUsuarioAsString().toLowerCase());

				}
				if (objetoListado instanceof TransaccionHistorica) {
					transaccionHistoricaLista = (TransaccionHistorica) objetoListado;
					/** tipos de transaccion * */
					mapaTransacciones.put(String
							.valueOf(transaccionHistoricaLista
									.getTipoTransaccion().getId()),
							transaccionHistoricaLista.getTipoTransaccion()
									.getDescripcion());
					/** secuencial de transacciones * */
					mapaIdTransacciones.put(
							String.valueOf(transaccionHistoricaLista
									.getSecuencial()), String
									.valueOf(transaccionHistoricaLista
											.getSecuencial()));
					/** cuentas cargadas * */
					cuentaAsString = ControlTransaccionFTP.obtenerCuentaCargo(
							transaccionLista.getId(), true);
					if (cuentaAsString != null && cuentaAsString.length() > 0) {
						mapaCuentasCargadas.put(cuentaAsString, cuentaAsString);
					}
					mapaCuentasCargadas.put(cuentaAsString, cuentaAsString);

					/** Clientes relacionados * */
					cliente = HibernateMap.queryByString(Cuenta.class,
							"where v.numero = '" + cuentaAsString + "'");
					if (cliente != null && cliente.size() > 0) {
						cuenta = (Cuenta) cliente.get(0);
						mapaIdClientes.put(String.valueOf(cuenta.getCliente()
								.getId()), cuenta.getCliente().getNombre());
					}
					/** Usuarios de creacion * */
					mapaUsuariosTransaccion.put(transaccionHistoricaLista
							.getUsuario().getUser(), transaccionHistoricaLista
							.getUsuario().getUsuarioAsString().toLowerCase());

				}
			}

			SortedMap mapaOrdenadoTrx = sortByValue(mapaTransacciones);
			SortedMap mapaOrdenadoIdClientes = sortByValue(mapaIdClientes);
			SortedMap mapaOrdenadoUsrTrx = sortByValue(mapaUsuariosTransaccion);

			consultaForm.setTiposTransaccion(mapaOrdenadoTrx);
			consultaForm.setListaIdTransaccion(mapaIdTransacciones);
			consultaForm.setCuentasCargadas(mapaCuentasCargadas);
			consultaForm.setUsuariosCreacion(mapaOrdenadoUsrTrx);
			consultaForm.setClientesTransaccion(mapaOrdenadoIdClientes);

			if (indicador == 2) {
				consultaForm.setPorMonto(false);
				consultaForm.setUsarMonto("1");
				consultaForm.setMontoDesde(0.0);
				consultaForm.setMontoHasta(0.0);
			} else {
				consultaForm.setPorMonto(consultaForm.getPorMonto());
				consultaForm.setUsarMonto(consultaForm.getUsarMonto());
				consultaForm.setMontoDesde(consultaForm.getMontoDesde());
				consultaForm.setMontoHasta(consultaForm.getMontoHasta());
			}

			consultaForm.setRegistros(true);
			consultaForm.getListaTransacciones().setCurrentPage(1);
			consultaForm.setRegistros(false);

		}
	}

	/**
	 * 
	 * @param session
	 * @param consultaForm
	 * @param mapping
	 * @param parametros
	 * @return
	 * @throws Exception
	 */
	private ActionForward executeConsultaDetallada(HttpSession session,
			ConsultaTransaccionForm consultaForm, ActionMapping mapping,
			ParametrosConsulta parametros) throws Exception {
		boolean completa = true;
		Transaccion transaccion = null;
		LineaTransaccion lineaDevolucion = null;
		EstadoTransaccion estado = consultaForm.getEstadoTransaccion();
		String horaBA = "";
		String horaACH = "";
		String diaHabil = "";
		String ventana = "";
		String fechaT = "";

		Usuario usuario = getUsuario(session);

		// ----Cargamos la transaccion
		{
			// si tenemos cargada una transaccion, debemos traerla del
			// estado que dice la transaccion
			// nos preocupamos por la salud del JVM
			{
				System.gc();
			}
			transaccion = TransaccionUtils.loadTransaccion(
					usuario.getCliente(), consultaForm.getIdTransaccion());

			// construye lista de LineaTransaccion sin aplicar
			{
				String str;
				LineaTransaccion linea;
				List list = new ArrayList();

				for (int i = 0; i < transaccion.getLineaTransaccionList()
						.size(); i++) {
					linea = transaccion.getLineaTransaccion(i);
					if ((i == transaccion.getLineaTransaccionList().size() - 1)
							&& linea.getFlags() == 3841) {
						lineaDevolucion = linea;
					}
					str = linea.getNumeroComprobante();
					if (str == null || str.trim().length() == 0) {
						list.add(linea);
					} else if (linea.getResultado() != 0) {
						list.add(linea);
					}
					// ID9214 - se agrega nueva validacion para determinar
					// cuando en una linea de cargo, la cuenta es null por no
					// existir en la BD
					if ((!transaccion.getTipoTransaccion().equals(
							TipoTransaccion.CARGOS_A_TERCEROS) && !transaccion
							.getTipoTransaccion().equals(
									TipoTransaccion.CARGOS_AUTOMATICOS))
							&& linea.esDebito() && linea.getCuenta() == null) {
						completa = false;
					}
				}
				consultaForm.setSinAplicarList(new LineaDetalleList(list));
			}

			System.out
					.println("cambiamos el list - ahora usamos uno de db (si es necesario)");
		}

		try {
			consultaForm.setMostrarBotonAutorizar(false);
			if (transaccion.getEstado().getPendienteAutorizar()) {
				// aqui revisamos si el usuario ya autorizo la transaccion
				Map map = getCuentaReglasMap(session);
				if (esUsuarioFirmante(usuario))
					consultaForm.setVerBotonEliminar(true);

				boolean puedeFirmar = AutorizacionesUtils
						.usuarioPuedeAutorizarTransaccion(transaccion, usuario,
								map);
				consultaForm.setMostrarBotonAutorizar(puedeFirmar);
			} else
				consultaForm.setVerBotonEliminar(false);
			// XXX ID9214 - Validamos si las lineas cargo estan completas
			// si no estan completa, se marca que la transaccion no
			// posee la cuenta de cargo OK
			transaccion.setTransaccionCuentaCargoOk(completa);

		} catch (AutorizationException e) {
			// si hay una exception es porque ya la firmo, asi que no mostramos
			// el boton
		}

		// ----Asignamos la transaccion para ser devuelta por el request
		consultaForm.setTransaccion(transaccion);

		// ----Buscamos Tipo de Planilla para planillas de salarios
		String tipoPlanilla = "";
		if (transaccion.getCamposExtension() != null
				&& transaccion.getCamposExtension().size() > 0) {
			TrxEncExt campoEnc = null;
			for (int x = 0; x < transaccion.getCamposExtension().size(); x++) {
				campoEnc = (TrxEncExt) transaccion.getCamposExtension().get(x);
				if (campoEnc.getCampo().equals(
						Constantes.CAMPO_EXT_TIPO_PLANILLA)) {
					tipoPlanilla = campoEnc.getValor();
					break;
				}
			}
		}
		consultaForm.setTipoPlanilla(tipoPlanilla);

		// ----Verificamos si tiene o no registros
		if (consultaForm.getTransaccion() == null) {
			// ----La transaccion no puede quedar null
			consultaForm.setTransaccion(new Transaccion());
			parametros.getMessages().add("consultatransaccion",
					new ActionMessage("errors.consulta.norows"));
		} else {
			// ----Cargamos los alias de las cuentas
			List lineasDetalle = LineaDetalleList
					.createLineaDetalleList(transaccion);
			consultaForm.getListaLineasTransaccion()
					.setRegistros(lineasDetalle);
			consultaForm.getListaLineasTransaccion().setCurrentPage(1);
			consultaForm.setListaLineasSinFiltrar(lineasDetalle);
		}

		/** Validaciones necesarias para mostrar campo token * */
		boolean usuarioUnico = TransaccionUtils.usuarioUnico(usuario);
		boolean esTrxToken = TransaccionUtils
				.esTransaccionPorToken(consultaForm.getTransaccion()
						.getTipoTransaccion());
		System.out.println("Es firma única el usuario: " + usuarioUnico);
		System.out.println("Es Trx por token: " + esTrxToken);

		if (usuarioUnico && esTrxToken)
			consultaForm.setMostrarCampoToken(true);
		else
			consultaForm.setMostrarCampoToken(false);

		/** Si la variable esTransaccionACHToken es true mostramos campo token * */
		if (TransaccionUtils.esTransaccionACHPorToken(transaccion
				.getTipoTransaccion().getId())) {
			consultaForm.setMostrarCampoToken(true);
			consultaForm.setTransaccionACH(true);
		}

		System.out.println("Muestro el campo token : "
				+ consultaForm.getMostrarCampoToken());

		/** VALIDACION PARA MOSTRAR MENSAJE LUEGO DE AUTORIZADA * */
		try {
			/** SI ES PENDIENTE DE APLICAR * */
			if (transaccion.getEstado().equals(
					EstadoTransaccion.ESTADO_PENDIENTE_APLICACION)) {
				if (TransaccionUtils.guardarExtensionTransaccion(0, transaccion
						.getTipoTransaccion().getId(), false)) {
					parametros
							.getMessages()
							.add(
									"consultatransaccion",
									new ActionMessage(
											"mensajes.transaccionprocesada.diahabilsiguiente.creacion"));
				}
				System.out.println("Validando Transaccion es ACH.");
				if (esTransaccionACH(transaccion.getTipoTransaccion())) {
					String ctaCargo = "";
					String id = Long.toString(transaccion.getId());
					try {
						List lista = HibernateMap.queryAllDepths(
								LineaTransaccion.class, "where transaccion = "
										+ id + " and tipo = 16");
						System.out.println(" Lista size = " + lista.size());
						Iterator iterator = lista.iterator();
						LineaTransaccion linea = null;
						while (iterator != null && iterator.hasNext()) {
							linea = ((LineaTransaccion) iterator.next());
							System.out.println("" + linea.toString());
							if (linea.esDebito()) {
								ctaCargo = linea.getCuentaAsString();
								linea = null;
								break;
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					// *** Agregar aqui mensaje explicativo del funcionamiento
					// del envio de
					HashMap hm = Servicios.getFechaYFondoDisponibleACH(usuario
							.getCliente().getId(),
							usuario.getUsuarioAsString(), ctaCargo, 1.0, 1.0,
							"", "");
					fechaT = String.valueOf(hm.get("fechaDisponible"));
					horaBA = Servicios.formatoHora((String) hm.get("horaBA"));
					horaACH = Servicios.formatoHora((String) hm.get("horaACH"));
					diaHabil = (String) hm.get("esHabil");
					ventana = (String) hm.get("ventana");

					if (hm != null) {
						String respError = (String) hm.get("error");
						if ("0".equals(respError)) {
							fechaT = new StringBuffer("").append(
									hm.get("fechaDisponible")).toString();

							// parametros.getMessages().add("consultatransaccion",
							// new
							// ActionMessage("mensajes.ach.diahabilsiguiente.creacion",
							// fechaT ));
							/*
							 * if(Servicios.primerEnvioACH()){//20140602 req
							 * 31763 Mejoras la proceso ACH
							 * parametros.getMessages().add("consultatransaccion",
							 * new ActionMessage("mensajes.ach.primerenvio",
							 * fechaT)); }else{
							 * parametros.getMessages().add("consultatransaccion",
							 * new ActionMessage("mensajes.ach.segundoenvio",
							 * fechaT)); }
							 */
						} else {
							consultaForm.setOcultarBotonAplicarACH(true);
							// parametros.getMessages().add("consultatransaccion",
							// new ActionMessage("errors.general.servicio.ach",
							// respError ));
						}
					} else {
						System.out.println("    Hasmap viene null -> ");
					}
					consultaForm.setTransaccionACH(true);
					long fechaProgramada = consultaForm.getTransaccion()
							.getFechaProgramacionAutomatica();

					// parametros.getMessages().add("consultatransaccion", new
					// ActionMessage("mensajes.ach.primerenvio"));
					if (diaHabil.equals("S")) {
						if (ventana.equals("4")) {
							parametros.getMessages().add(
									"consultatransaccion",
									new ActionMessage(
											"mensajes.ach.ventanahoraria2",
											horaBA, horaACH));
						} else {
							parametros.getMessages().add(
									"consultatransaccion",
									new ActionMessage(
											"mensajes.ach.ventanahoraria",
											horaBA, horaACH));
						}
					} else {
						parametros.getMessages().add(
								"consultatransaccion",
								new ActionMessage("mensajes.ach.dianohabil",
										horaACH, fechaT));
					}

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		// mbpalaci
		if (transaccion.getTipoTransaccion().equals(
				TipoTransaccion.COTIZACION_DE_MONEDA_EN_LINEA)
				&& transaccion.getEstado().equals(
						EstadoTransaccion.ESTADO_APLICADA)) {
			consultaForm.setFechaVencimiento(transaccion.getLineaTransaccion(0)
					.getTramaSalidaAsString());
		}

		// Determinamos si hay que mostrar mensaje del tiempo disponible para la
		// cotizacion de moneda //mbpalaci
		if (consultaForm.getTransaccion().getTipoTransaccion().equals(
				TipoTransaccion.COTIZACION_DE_MONEDA_EN_LINEA)) {
			LineaTransaccion linea = consultaForm.getTransaccion()
					.getLineaTransaccion(0);

			if (consultaForm.getTransaccion().getEstado().equals(
					EstadoTransaccion.ESTADO_APLICADA)) {
				consultaForm.setMostrarTrideTicket(true);
				consultaForm.setMostrarTiempoDisponible(false);
				consultaForm.setTrideTicket(linea.getNumeroComprobante());
				consultaForm
						.setFechaVencimiento(linea.getTramaSalidaAsString());
			} else {
				consultaForm.setMostrarTrideTicket(false);
				// consultaForm.setTiempoDisponible(linea.getMemoAsString());
				SimpleDateFormat ds = new SimpleDateFormat("hh:mm:ss");
				ds.setTimeZone(TimeZone.getTimeZone("UTC"));
				Date date = ds.parse(linea.getMemoAsString());

				System.out.println("hora en linea de transaccion: "
						+ linea.getMemoAsString());
				System.out.println("date: " + date.toString());
				Date date1 = new Date(date.getTime()
						- (System.currentTimeMillis() - consultaForm
								.getTransaccion().getFechaCreacion()));
				System.out.println("date1: " + date1);
				DateFormat dt = new SimpleDateFormat("hh:mm:ss");
				// String hora = dt.format(date1);
				Calendar c = Calendar.getInstance();
				c.setTime(date);
				c.add(Calendar.HOUR_OF_DAY, date.getHours());
				c.add(Calendar.MINUTE, date.getMinutes());
				c.add(Calendar.SECOND, date.getSeconds());
				long tiempoTranscurrido = (System.currentTimeMillis() - consultaForm
						.getTransaccion().getFechaCreacion());
				// Date date1 = new Date(tiempoTranscurrido);
				System.out.println("<<---fechaCreacion---->> : "
						+ consultaForm.getTransaccion().getFechaCreacion());
				System.out.println("<<---tiempoActual----->> : "
						+ System.currentTimeMillis());
				System.out.println("<<---Transcurrido----->> : "
						+ (System.currentTimeMillis() - consultaForm
								.getTransaccion().getFechaCreacion()));
				System.out.println("<<---Limite----------->> : "
						+ date.getTime());

				if ((tiempoTranscurrido > date.getTime())
						&& !consultaForm.getTransaccion().getEstado().equals(
								EstadoTransaccion.ESTADO_RECHAZADA)) {
					consultaForm.getTransaccion().setEstado(
							EstadoTransaccion.ESTADO_RECHAZADA);
					linea
							.setMensajeDevolucion(MQMensajeDevolucion.COTIZACION_VENCIDA);
					consultaForm.setMostrarTiempoDisponible(false);
					// TransaccionUtils.save(null,
					// consultaForm.getTransaccion(), false);

					consultaForm.getTransaccion().getLineaTransaccionList()
							.add(linea);
					HibernateMap.update(consultaForm.getTransaccion());
					HibernateMap.update(linea);
					TransaccionUtils.pasarTransaccionAHistorica(consultaForm
							.getTransaccion());
					consultaForm.setMostrarBotonAutorizar(false);
				} else {
					if (consultaForm.getTransaccion().getEstado().equals(
							EstadoTransaccion.ESTADO_RECHAZADA)) {
						consultaForm.setTiempoDisponible("00:00:00");
						consultaForm.setMostrarTiempoDisponible(true);
					} else {
						String hora = "";
						String minutos = "";
						String segundos = "";
						if ((date1.getHours() - date.getHours()) < 10) {
							hora = "0" + (date1.getHours() - date.getHours());
						} else {
							hora = "" + (date1.getHours() - date.getHours());
						}
						if (date1.getMinutes() < 10) {
							minutos = "0" + date1.getMinutes();
						} else {
							minutos = "" + date1.getMinutes();
						}
						if (date1.getSeconds() < 10) {
							segundos = "0" + date1.getSeconds();
						} else {
							segundos = "" + date1.getSeconds();
						}
						// consultaForm.setTiempoDisponible(""+(date1.getHours()-date.getHours())+":"+date1.getMinutes()+":"+date1.getSeconds());
						consultaForm.setTiempoDisponible(hora + ":" + minutos
								+ ":" + segundos);
						consultaForm.setMostrarTiempoDisponible(true);
					}
				}

			}
		} else {
			consultaForm.setMostrarTrideTicket(false);
			consultaForm.setMostrarTiempoDisponible(false);
		}

		// ----Guardamos un form de comprobantes
		ConsultaComprobantesForm formComprobante = new ConsultaComprobantesForm();
		formComprobante.setTransaccion(consultaForm.getTransaccion());
		formComprobante.setLinea(lineaDevolucion);
		// ----Guardamos el form de comprobantes
		session.setAttribute(SessionKeys.KEY_COMPROBANTES, formComprobante);
		session
				.setAttribute(SessionKeys.KEY_CONSULTA_TRANSACCION,
						consultaForm);
		return mapping.findForward("verdetalle");

	}

	/**
	 * Obtiene la información de la transacción, incluyendo las lineas de
	 * detalle con paginación a la base de datos.
	 * 
	 * @param session
	 * @param consultaForm
	 * @param mapping
	 * @param parametros
	 * @param limit
	 * @param offset
	 * @return
	 * @throws Exception
	 */
	private ActionForward executeConsultaDetalladaPaginada(HttpSession session,
			ConsultaTransaccionForm consultaForm, ActionMapping mapping,
			ParametrosConsulta parametros, int limit, int offset, String filtro)
			throws Exception {
		boolean completa = true;
		Transaccion transaccion = null;
		Transaccion transaccionCompleta = null;
		LineaTransaccion lineaDevolucion = null;
		EstadoTransaccion estado = consultaForm.getEstadoTransaccion();
		consultaForm.setMostrarDetalleACH(false);

		boolean esTransaccionToken = false;
		boolean esUsuarioUnico = false;
		boolean esTransaccionACH = false;
		String horaBA = "";
		String horaACH = "";
		String diaHabil = "";
		String ventana = "";
		String fechaT = "";

		Usuario usuario = getUsuario(session);

		// ----Cargamos la transaccion
		{
			// si tenemos cargada una transaccion, debemos traerla del
			// estado que dice la transaccion
			// nos preocupamos por la salud del JVM
			{
				System.gc();
			}
			String filtroLineas = armarFiltroLineas(filtro);
			transaccion = TransaccionUtils.loadTransaccionPaginada(usuario
					.getCliente(), consultaForm.getIdTransaccion(), limit,
					offset, filtroLineas, null);
			System.out.println("tipo trx: " + transaccion.getTipoTransaccion());
			if (transaccion.getTipoTransaccion().equals(
					TipoTransaccion.PAGADURIAS_ACH)
					|| transaccion.getTipoTransaccion().equals(
							TipoTransaccion.PAGO_DE_PENSIONES_ACH)
					|| transaccion.getTipoTransaccion().equals(
							TipoTransaccion.PAGO_PLANILLA_ACH)
					|| transaccion.getTipoTransaccion().equals(
							TipoTransaccion.PAGO_PROVEEDORES_ACH)) {
				transaccionCompleta = TransaccionUtils.loadTransaccion(usuario
						.getCliente(), consultaForm.getIdTransaccion());
			}
			// Se obtiene la cantidad de lineas basado en la transacción
			// if
			// (consultaForm.getTransaccion().getEstado().equals(EstadoTransaccion.ESTADO_APLICADA)
			// ||
			// consultaForm.getTransaccion().getEstado().equals(EstadoTransaccion.ESTADO_RECHAZADA)
			// ||
			// consultaForm.getTransaccion().getEstado().equals(EstadoTransaccion.ESTADO_ELIMINADA))
			// {
			if (transaccion.getEstado().equals(
					EstadoTransaccion.ESTADO_APLICADA)
					|| transaccion.getEstado().equals(
							EstadoTransaccion.ESTADO_RECHAZADA)
					|| transaccion.getEstado().equals(
							EstadoTransaccion.ESTADO_ELIMINADA)
					|| transaccion.getEstado().equals(
							EstadoTransaccion.ESTADO_APLICADO_PARCIAL_ACH)) {
				filtroLineas = "transaccionHistorica = " + transaccion.getId()
						+ filtroLineas;
			} else {
				filtroLineas = "transaccion = " + transaccion.getId()
						+ filtroLineas;
			}
			int cantidadLineas = TransaccionUtils
					.obtenerConteoLineasTransaccion(filtroLineas);
			consultaForm.setSizeLineas(cantidadLineas);
			// construye lista de LineaTransaccion sin aplicar
			{
				String str;
				LineaTransaccion linea;
				List list = new ArrayList();

				for (int i = 0; i < transaccion.getLineaTransaccionList()
						.size(); i++) {
					linea = transaccion.getLineaTransaccion(i);
					if ((i == transaccion.getLineaTransaccionList().size() - 1)
							&& linea.getFlags() == 3841) {
						lineaDevolucion = linea;
					}
					str = linea.getNumeroComprobante();
					if (str == null || str.trim().length() == 0) {
						list.add(linea);
					} else if (linea.getResultado() != 0) {
						list.add(linea);
					}
					// ID9214 - se agrega nueva validacion para determinar
					// cuando en una linea de cargo, la cuenta es null por no
					// existir en la BD
					if ((!transaccion.getTipoTransaccion().equals(
							TipoTransaccion.CARGOS_A_TERCEROS) && !transaccion
							.getTipoTransaccion().equals(
									TipoTransaccion.CARGOS_AUTOMATICOS))
							&& linea.esDebito() && linea.getCuenta() == null) {
						completa = false;
					}
				}
				consultaForm.setSinAplicarList(new LineaDetalleList(list));
			}

			System.out
					.println("cambiamos el list - ahora usamos uno de db (si es necesario)");
		}

		try {
			consultaForm.setMostrarBotonAutorizar(false);

			// ----------------------------------------------
			// Se agrega validacion de transacciones pendientes con estado
			// aplicando y aplicada.
			if (transaccion.getTipoTransaccion().equals(
					TipoTransaccion.PAGO_PLANILLA)) {
				if (transaccion.getEstado().equals(
						EstadoTransaccion.ESTADO_PENDIENTE_APLICACION)
						|| transaccion
								.getEstado()
								.equals(
										EstadoTransaccion.ESTADO_PENDIENTE_AUTORIZACION)) {
					Servicios.validarPlanillasPendientes(transaccion,
							parametros, usuario);
				}
			}
			if (!esTransaccionToken
					&& esUsuarioUnico
					&& TransaccionUtils.esTransaccionPorToken(transaccion
							.getTipoTransaccion())) {
				esTransaccionToken = true;
			}
			if (!esTransaccionACH
					&& TransaccionUtils.esTransaccionACHPorToken(transaccion
							.getTipoTransaccion().getId())) {
				esTransaccionACH = true;
			}

			/**
			 * Si la variable esTransaccionToken es true mostramos campo de lo
			 * contrario no *
			 */
			if (esTransaccionToken) {
				consultaForm.setMostrarCampoToken(true);
			}
			/** Si la variable esTransaccionACH es true mostramos campo token * */
			else if (esTransaccionACH) {
				consultaForm.setTransaccionACH(true);
			} else {
				consultaForm.setTransaccionACH(false);
			}

			if (transaccion.getEstado().getPendienteAutorizar()) {
				// aqui revisamos si el usuario ya autorizo la transaccion
				Map map = getCuentaReglasMap(session);
				if (esUsuarioFirmante(usuario))
					consultaForm.setVerBotonEliminar(true);

				boolean puedeFirmar = AutorizacionesUtils
						.usuarioPuedeAutorizarTransaccion(transaccion, usuario,
								map);
				consultaForm.setMostrarBotonAutorizar(puedeFirmar);
			} else
				consultaForm.setVerBotonEliminar(false);
			// XXX ID9214 - Validamos si las lineas cargo estan completas
			// si no estan completa, se marca que la transaccion no
			// posee la cuenta de cargo OK
			transaccion.setTransaccionCuentaCargoOk(completa);

		} catch (AutorizationException e) {
			// si hay una exception es porque ya la firmo, asi que no mostramos
			// el boton
		}

		// ----Asignamos la transaccion para ser devuelta por el request
		consultaForm.setTransaccion(transaccion);

		// ----Buscamos Tipo de Planilla para planillas de salarios
		String tipoPlanilla = "";
		if (transaccion.getCamposExtension() != null
				&& transaccion.getCamposExtension().size() > 0) {
			TrxEncExt campoEnc = null;
			for (int x = 0; x < transaccion.getCamposExtension().size(); x++) {
				campoEnc = (TrxEncExt) transaccion.getCamposExtension().get(x);
				if (campoEnc.getCampo().equals(
						Constantes.CAMPO_EXT_TIPO_PLANILLA)) {
					tipoPlanilla = campoEnc.getValor();
					break;
				}
			}
		}
		consultaForm.setTipoPlanilla(tipoPlanilla);

		// ----Verificamos si tiene o no registros
		if (consultaForm.getTransaccion() == null) {
			// ----La transaccion no puede quedar null
			consultaForm.setTransaccion(new Transaccion());
			parametros.getMessages().add("consultatransaccion",
					new ActionMessage("errors.consulta.norows"));
		} else {
			// ----Cargamos los alias de las cuentas
			List lineasDetalle = LineaDetalleList
					.createLineaDetalleList(transaccion);
			consultaForm.getListaLineasTransaccion()
					.setRegistros(lineasDetalle);
			consultaForm.getListaLineasTransaccion().setCurrentPage(1);
			consultaForm.setListaLineasSinFiltrar(lineasDetalle);
		}

		if (esTransaccionACH(transaccion.getTipoTransaccion())) {
			if (transaccion.getEstado().equals(
					EstadoTransaccion.ESTADO_APLICADA)) {
				String fechaACH = "";
				List encabezadoTrxACH = HibernateMap.queryByString(
						TrxEncExt.class,
						" where v.campo = 'FECHA_APLIC_ACH' and v.idTransaccion = "
								+ transaccion.getId());
				if (encabezadoTrxACH != null && !encabezadoTrxACH.isEmpty()) {
					TrxEncExt trxEncExtACH = (TrxEncExt) encabezadoTrxACH
							.get(0);
					fechaACH = trxEncExtACH.getValor();
				}
				// parametros.getMessages().add("consultatransaccion", new
				// ActionMessage("mensajes.ach.diahabilsiguiente.creacion",
				// fechaACH ));
				// parametros.getMessages().add("consultatransaccion", new
				// ActionMessage("mensajes.ach.aplico"));

				String ctaCargo = "";
				String id = Long.toString(transaccion.getId());
				try {
					List lista = HibernateMap.queryAllDepths(
							LineaTransaccion.class, "where transaccion = " + id
									+ " and tipo = 16");
					System.out.println(" Lista size = " + lista.size());
					Iterator iterator = lista.iterator();
					LineaTransaccion linea = null;
					while (iterator != null && iterator.hasNext()) {
						linea = ((LineaTransaccion) iterator.next());
						System.out.println("" + linea.toString());
						if (linea.esDebito()) {
							ctaCargo = linea.getCuentaAsString();
							linea = null;
							break;
						}

					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				HashMap hm = Servicios.getFechaYFondoDisponibleACH(usuario
						.getCliente().getId(), usuario.getUsuarioAsString(),
						ctaCargo, 1.0, 1.0, "", "");
				fechaT = String.valueOf(hm.get("fechaDisponible"));
				horaBA = Servicios.formatoHora((String) hm.get("horaBA"));
				horaACH = Servicios.formatoHora((String) hm.get("horaACH"));
				diaHabil = (String) hm.get("esHabil");
				ventana = (String) hm.get("ventana");
				consultaForm.setTransaccionACH(true);

				if (hm != null) {
					String respError = (String) hm.get("error");
					if ("0".equals(respError)) {
						// fechaT = new
						// StringBuffer("").append(hm.get("fechaDisponible")).toString();
						// parametros.getMessages().add("consultatransaccion",
						// new
						// ActionMessage("mensajes.ach.diahabilsiguiente.creacion",
						// fechaT ));
					} else {
						consultaForm.setOcultarBotonAplicarACH(true);
						// parametros.getMessages().add("consultatransaccion",
						// new ActionMessage("errors.general.servicio.ach",
						// respError ));
					}
				} else {
					System.err.println("    Hasmap viene null -> ");
				}
				consultaForm.setTransaccionACH(true);
				// parametros.getMessages().add("consultatransaccion", new
				// ActionMessage("mensajes.ach.primerenvio"));

				long fechaProgramada = consultaForm.getTransaccion()
						.getFechaProgramacionAutomatica();

				if (diaHabil.equals("S")) {
					if (ventana.equals("4")) {
						parametros.getMessages().add(
								"consultatransaccion",
								new ActionMessage(
										"mensajes.ach.ventanahoraria2", horaBA,
										horaACH));
					} else {
						parametros.getMessages().add(
								"consultatransaccion",
								new ActionMessage(
										"mensajes.ach.ventanahoraria", horaBA,
										horaACH));
					}
				} else {
					parametros.getMessages().add(
							"consultatransaccion",
							new ActionMessage("mensajes.ach.dianohabil",
									horaACH, fechaT));
				}

			}

			if (transaccion.getEstado().equals(
					EstadoTransaccion.ESTADO_PENDIENTE_APLICACION)) {
				if (esTransaccionACH(transaccion.getTipoTransaccion())) {
					String ctaCargo = "";
					String id = Long.toString(transaccion.getId());
					try {
						List lista = HibernateMap.queryAllDepths(
								LineaTransaccion.class, "where transaccion = "
										+ id + " and tipo = 16");
						System.out.println(" Lista size = " + lista.size());
						Iterator iterator = lista.iterator();
						LineaTransaccion linea = null;
						while (iterator != null && iterator.hasNext()) {
							linea = ((LineaTransaccion) iterator.next());
							System.out.println("" + linea.toString());
							if (linea.esDebito()) {
								ctaCargo = linea.getCuentaAsString();
								linea = null;
								break;
							}

						}
					} catch (Exception e) {
						e.printStackTrace();
					}

					HashMap hm = Servicios.getFechaYFondoDisponibleACH(usuario
							.getCliente().getId(),
							usuario.getUsuarioAsString(), ctaCargo, 1.0, 1.0,
							"", "");
					fechaT = String.valueOf(hm.get("fechaDisponible"));
					horaBA = Servicios.formatoHora((String) hm.get("horaBA"));
					horaACH = Servicios.formatoHora((String) hm.get("horaACH"));
					diaHabil = (String) hm.get("esHabil");
					ventana = (String) hm.get("ventana");
					consultaForm.setTransaccionACH(true);

					if (hm != null) {
						String respError = (String) hm.get("error");
						if ("0".equals(respError)) {
							// fechaT = new
							// StringBuffer("").append(hm.get("fechaDisponible")).toString();
							// parametros.getMessages().add("consultatransaccion",
							// new
							// ActionMessage("mensajes.ach.diahabilsiguiente.creacion",
							// fechaT ));
						} else {
							consultaForm.setOcultarBotonAplicarACH(true);
							// parametros.getMessages().add("consultatransaccion",
							// new ActionMessage("errors.general.servicio.ach",
							// respError ));
						}
					} else {
						System.err.println("    Hasmap viene null -> ");
					}
					consultaForm.setTransaccionACH(true);
					// parametros.getMessages().add("consultatransaccion", new
					// ActionMessage("mensajes.ach.primerenvio"));
					if (diaHabil.equals("S")) {
						if (ventana.equals("4")) {

							parametros.getMessages().add(
									"consultatransaccion",
									new ActionMessage(
											"mensajes.ach.ventanahoraria2",
											horaBA, horaACH));

						} else {

							parametros.getMessages().add(
									"consultatransaccion",
									new ActionMessage(
											"mensajes.ach.ventanahoraria",
											horaBA, horaACH));
						}
					} else {

						parametros.getMessages().add(
								"consultatransaccion",
								new ActionMessage("mensajes.ach.dianohabil",
										horaACH, fechaT));
					}
				}
			}
			// PENDIENTES DE AUTORIZAR 20140619 req 31763 Mejoras la proceso ACH
			if (transaccion.getEstado().equals(
					EstadoTransaccion.ESTADO_PENDIENTE_AUTORIZACION)) {
				if (esTransaccionACH(transaccion.getTipoTransaccion())) {
					String ctaCargo = "";
					String id = Long.toString(transaccion.getId());
					try {
						List lista = HibernateMap.queryAllDepths(
								LineaTransaccion.class, "where transaccion = "
										+ id + " and tipo = 16");
						System.out.println(" Lista size = " + lista.size());
						Iterator iterator = lista.iterator();
						LineaTransaccion linea = null;
						while (iterator != null && iterator.hasNext()) {
							linea = ((LineaTransaccion) iterator.next());
							System.out.println("" + linea.toString());
							if (linea.esDebito()) {
								ctaCargo = linea.getCuentaAsString();
								linea = null;
								break;
							}

						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					long fechaProgramada = consultaForm.getTransaccion()
							.getFechaProgramacionAutomatica();

					HashMap hm = Servicios.getFechaYFondoDisponibleACH(usuario
							.getCliente().getId(),
							usuario.getUsuarioAsString(), ctaCargo, 1.0, 1.0,
							"", "");
					// String fechaT = "";
					fechaT = String.valueOf(hm.get("fechaDisponible"));
					horaBA = Servicios.formatoHora((String) hm.get("horaBA"));
					horaACH = Servicios.formatoHora((String) hm.get("horaACH"));
					diaHabil = (String) hm.get("esHabil");
					ventana = (String) hm.get("ventana");
					consultaForm.setTransaccionACH(true);

					if (hm != null) {
						String respError = (String) hm.get("error");
						if ("0".equals(respError)) {
							fechaT = new StringBuffer("").append(
									hm.get("fechaDisponible")).toString();
							// parametros.getMessages().add("consultatransaccion",
							// new
							// ActionMessage("mensajes.ach.diahabilsiguiente.creacion",
							// fechaT ));
							/*
							 * if(Servicios.primerEnvioACH()){//20140602 req
							 * 31763 Mejoras la proceso ACH
							 * parametros.getMessages().add("transferenciasACH",
							 * new ActionMessage("mensajes.ach.primerenvio",
							 * fechaT)); }else{
							 * parametros.getMessages().add("transferenciasACH",
							 * new ActionMessage("mensajes.ach.segundoenvio",
							 * fechaT)); }
							 */
						} else {
							consultaForm.setOcultarBotonAplicarACH(true);
							// parametros.getMessages().add("consultatransaccion",
							// new ActionMessage("errors.general.servicio.ach",
							// respError ));
						}
					} else {
						System.err.println("    Hasmap viene null -> ");
					}
					// parametros.getMessages().add("consultatransaccion", new
					// ActionMessage("mensajes.ach.primerenvio"));

					// System.out.println("fecha Programacion: "+new
					// Date(fechaProgramada)+"- "+fechaProgramada);

					if (fechaProgramada > 0) {
						Date date = new Date(fechaProgramada);
						System.out.println("fecha programada: "
								+ date.toString());
						String fecha = (date.getYear() + 1900)
								+ ""
								+ Helper.completeLeftWith(""
										+ (date.getMonth() + 1), '0', 2)
								+ ""
								+ Helper.completeLeftWith("" + date.getDate(),
										'0', 2);
						System.out.println("fecha: " + fecha);
						String hora = Helper.completeLeftWith(""
								+ date.getHours(), '0', 2)
								+ Helper.completeLeftWith(""
										+ date.getMinutes(), '0', 2) + "00";
						HashMap hm2 = Servicios.getFechaYFondoDisponibleACH(
								usuario.getCliente().getId(), usuario
										.getUsuarioAsString(), ctaCargo, 1.0,
								1.0, fecha, hora);
						fechaT = String.valueOf(hm2.get("fechaDisponible"));
						horaBA = Servicios.formatoHora((String) hm2
								.get("horaBA"));
						horaACH = Servicios.formatoHora((String) hm2
								.get("horaACH"));
						diaHabil = (String) hm2.get("esHabil");
						ventana = (String) hm2.get("ventana");

						parametros.getMessages()
								.add(
										"consultatransaccion",
										new ActionMessage(
												"mensajes.ach.programadas",
												fechaT.replaceAll("\\/", "-"),
												horaACH));

					} else {
						if (diaHabil.equals("S")) {
							if (ventana.equals("4")) {

								parametros.getMessages().add(
										"consultatransaccion",
										new ActionMessage(
												"mensajes.ach.ventanahoraria2",
												horaBA, horaACH));

							} else {

								parametros.getMessages().add(
										"consultatransaccion",
										new ActionMessage(
												"mensajes.ach.ventanahoraria",
												horaBA, horaACH));
							}
						} else {

							parametros.getMessages().add(
									"consultatransaccion",
									new ActionMessage(
											"mensajes.ach.dianohabil", horaACH,
											fechaT));
						}
					}
				}
			}

		}

		// verifica si la transaccion es transferencias propias con cargo a tdc
		// para mostrar el mensaje
		consultaForm.setMostrarMensajeTransPropiaTCD(false);
		if (consultaForm.getTransaccion().getTipoTransaccion().equals(
				TipoTransaccion.TRANSFERENCIA_CUENTAS_PROPIAS)
				&& consultaForm.getMostrarBotonAutorizar()) {
			List lineaTransaccionList = consultaForm.getTransaccion()
					.getLineaTransaccionList();
			for (Iterator iter = lineaTransaccionList.iterator(); iter
					.hasNext();) {
				LineaTransaccion linea = (LineaTransaccion) iter.next();
				if (linea.esDebito()
						&& linea
								.getCuenta()
								.getProducto()
								.getTipoProducto()
								.equals(
										TipoProductoUtils.PRODUCT_TARJETA_CREDITO)) {
					consultaForm.setMostrarMensajeTransPropiaTCD(true);
				}
			}
		}

		// validamos si es una solicitud de transferencia internacional
		if (consultaForm.getTransaccion().getTipoTransaccion().equals(
				TipoTransaccion.SOLICITUD_TRANSFERENCIA_INTERNACIONAL)) {
			LineaTransaccion lineaAplicar = null;
			SolicitudTransferenciaInternacional solicitud = new SolicitudTransferenciaInternacional();
			Iterator iter = consultaForm.getTransaccion()
					.getLineaTransaccionList().iterator();
			while (iter.hasNext()) {
				lineaAplicar = (LineaTransaccion) iter.next();
				if (lineaAplicar.getFlags() == TransaccionFlags.NORMAL)
					break;
			}
			solicitud.fromBytes(lineaAplicar.getMemoAsBytes());
			consultaForm.setSolicitud(solicitud);
			consultaForm.setMostrarDetalleSolicitudTransInternacional(true);
		} else {
			consultaForm.setMostrarDetalleSolicitudTransInternacional(false);
		}

		// validamos si es una actualizacion de datos de usuarios
		if (consultaForm.getTransaccion().getTipoTransaccion().equals(
				TipoTransaccion.DATOS_USUARIO)) {
			DatosUsuariosDB datosUsuarioDB = (DatosUsuariosDB) DatosUsuariosDB
					.readObject(consultaForm.getTransaccion()
							.getLineaTransaccion(0).getMemoAsBytes());
			datosUsuarioDB
					.setUsuario(datosUsuarioDB.getUsuario().split("]")[1]);
			consultaForm.setDatosUsuarioDB(datosUsuarioDB);
			consultaForm.setMostrarDetalleDatosUsuario(true);
		} else {
			consultaForm.setMostrarDetalleDatosUsuario(false);
		}

		// validamos si es una actualizacion de datos de usuarios
		if (consultaForm.getTransaccion().getTipoTransaccion().equals(
				TipoTransaccion.DESEMBOLSO_EN_LINEA_LC)
				&& (transaccion.getEstado().equals(
						EstadoTransaccion.ESTADO_PENDIENTE_APLICACION) || transaccion
						.getEstado()
						.equals(EstadoTransaccion.ESTADO_PENDIENTE_AUTORIZACION))) {
			DatosConvenioDesembLC datosConvenioDesembLC = (DatosConvenioDesembLC) DatosConvenioDesembLC
					.readObject(consultaForm.getTransaccion()
							.getLineaTransaccion(0).getMemoAsBytes());
			consultaForm.setDatosConvenioDesembLC(datosConvenioDesembLC);
			consultaForm.setMostrarDetalleDesembolsoLCred(true);
		} else {
			consultaForm.setMostrarDetalleDesembolsoLCred(false);
		}
		// validamos si es una reserva de fondos de pagoes
		if (consultaForm.getTransaccion().getTipoTransaccion().equals(
				TipoTransaccion.RESERVA)) {
			LineaTransaccion linea = consultaForm.getTransaccion()
					.getLineaTransaccion(0);
			consultaForm.setFechaVencimientoReserva(linea.getMemoAsString());
			consultaForm.setMostrarFechaVencimiento(true);
		} else {
			consultaForm.setMostrarFechaVencimiento(false);
		}

		// validamos si es liberacion de fondos de pagoes
		if (consultaForm.getTransaccion().getTipoTransaccion().equals(
				TipoTransaccion.LIBERACION_DE_FONDOS)) {
			LineaTransaccion linea = consultaForm.getTransaccion()
					.getLineaTransaccion(0);
			consultaForm.setMontoReserva(linea.getMonto());
			consultaForm.setNumReserva(linea.getNumeroComprobante());
			consultaForm.setFechaCreacionReserva(linea.getMemoAsString());
			consultaForm.setFechaVencimientoReserva(linea
					.getTramaEntradaAsString());
			consultaForm.setMostrarDetalleLiberacionFondos(true);
		} else {
			consultaForm.setMostrarDetalleLiberacionFondos(false);
		}

		// verificamos si es adicion o eliminacion de cuentas predefinida
		consultaForm.setCuentasPredefinidas(false);
		consultaForm.setMostrarMensajeNitCtoPredefinidas(false);
		if (consultaForm.getTransaccion().getTipoTransaccion().equals(
				TipoTransaccion.ADICION_CUENTAS_PREDEFINIDAS)
				|| consultaForm.getTransaccion().getTipoTransaccion().equals(
						TipoTransaccion.ELIMINAR_CUENTAS_PREDEFINIDAS)) {
			List lineaTransaccionList = consultaForm.getTransaccion()
					.getLineaTransaccionList();
			for (Iterator iter = lineaTransaccionList.iterator(); iter
					.hasNext();) {
				LineaTransaccion linea = (LineaTransaccion) iter.next();
				if (linea.getReferencia().startsWith("NIT")) {
					consultaForm.setMostrarMensajeNitCtoPredefinidas(true);
				}
			}
			consultaForm.setCuentasPredefinidas(true);
		}

		// Determinamos si hay que mostrar mensaje de que aplica impuesto
		consultaForm.setMostrarMensajeImpuesto(aplicaImpuesto(transaccion));
		System.out
				.println("ConsultaTransaccionAction.executeConsultaDetalladaPaginada(), aplicaImpuesto = "
						+ consultaForm.getMostrarMensajeImpuesto());
		if (consultaForm.getMostrarMensajeImpuesto()) {
			List lineaTransaccionList = consultaForm.getTransaccion()
					.getLineaTransaccionList();
			for (Iterator iter = lineaTransaccionList.iterator(); iter
					.hasNext();) {
				LineaTransaccion linea = (LineaTransaccion) iter.next();
				if (linea.esDebito()) {
					if (linea.getFlags() == TransaccionFlags.IMPUESTO) {
						consultaForm.setMontoImpuesto(linea.getMonto());
						break;
					}
				}
			}
		} else {
			consultaForm.setMontoImpuesto(0.00);
		}

		// Determinamos si hay que mostrar mensaje del tiempo disponible para la
		// cotizacion de moneda //mbpalaci
		if (consultaForm.getTransaccion().getTipoTransaccion().equals(
				TipoTransaccion.COTIZACION_DE_MONEDA_EN_LINEA)) {
			LineaTransaccion linea = consultaForm.getTransaccion()
					.getLineaTransaccion(0);

			if (consultaForm.getTransaccion().getEstado().equals(
					EstadoTransaccion.ESTADO_APLICADA)) {
				consultaForm.setMostrarTrideTicket(true);
				consultaForm.setMostrarTiempoDisponible(false);
				consultaForm.setTrideTicket(linea.getNumeroComprobante());
				consultaForm
						.setFechaVencimiento(linea.getTramaSalidaAsString());
			} else {
				consultaForm.setMostrarTrideTicket(false);
				// consultaForm.setTiempoDisponible(linea.getMemoAsString());
				SimpleDateFormat ds = new SimpleDateFormat("hh:mm:ss");
				ds.setTimeZone(TimeZone.getTimeZone("UTC"));

				Date date = ds.parse(linea.getMemoAsString());

				System.out.println("hora en linea de transaccion: "
						+ linea.getMemoAsString());
				System.out.println("date: " + date.toString());
				Date date1 = new Date(date.getTime()
						- (System.currentTimeMillis() - consultaForm
								.getTransaccion().getFechaCreacion()));
				System.out.println("date1: " + date1);
				DateFormat dt = new SimpleDateFormat("hh:mm:ss");
				// String hora = dt.format(date1);
				Calendar c = Calendar.getInstance();
				c.setTime(date);
				c.add(Calendar.HOUR_OF_DAY, date.getHours());
				c.add(Calendar.MINUTE, date.getMinutes());
				c.add(Calendar.SECOND, date.getSeconds());
				long tiempoTranscurrido = (System.currentTimeMillis() - consultaForm
						.getTransaccion().getFechaCreacion());
				// Date date1 = new Date(tiempoTranscurrido);
				System.out.println("<<---fechaCreacion---->> : "
						+ consultaForm.getTransaccion().getFechaCreacion());
				System.out.println("<<---tiempoActual----->> : "
						+ System.currentTimeMillis());
				System.out.println("<<---Transcurrido----->> : "
						+ (System.currentTimeMillis() - consultaForm
								.getTransaccion().getFechaCreacion()));
				System.out.println("<<---Limite----------->> : "
						+ date.getTime());

				if ((tiempoTranscurrido > date.getTime())
						&& !consultaForm.getTransaccion().getEstado().equals(
								EstadoTransaccion.ESTADO_RECHAZADA)) {
					consultaForm.getTransaccion().setEstado(
							EstadoTransaccion.ESTADO_RECHAZADA);
					linea
							.setMensajeDevolucion(MQMensajeDevolucion.COTIZACION_VENCIDA);
					consultaForm.setMostrarTiempoDisponible(false);
					// TransaccionUtils.save(null,
					// consultaForm.getTransaccion(), false);

					// consultaForm.getTransaccion().getLineaTransaccionList().add(linea);
					HibernateMap.update(consultaForm.getTransaccion());
					HibernateMap.update(linea);
					TransaccionUtils.pasarTransaccionAHistorica(consultaForm
							.getTransaccion());
					consultaForm.setMostrarBotonAutorizar(false);
				} else {
					if (consultaForm.getTransaccion().getEstado().equals(
							EstadoTransaccion.ESTADO_RECHAZADA)) {
						consultaForm.setTiempoDisponible("00:00:00");
						consultaForm.setMostrarTiempoDisponible(true);
					} else {
						String hora = "";
						String minutos = "";
						String segundos = "";
						if ((date1.getHours() - date.getHours()) < 10) {
							hora = "0" + (date1.getHours() - date.getHours());
						} else {
							hora = "" + (date1.getHours() - date.getHours());
						}
						if (date1.getMinutes() < 10) {
							minutos = "0" + date1.getMinutes();
						} else {
							minutos = "" + date1.getMinutes();
						}
						if (date1.getSeconds() < 10) {
							segundos = "0" + date1.getSeconds();
						} else {
							segundos = "" + date1.getSeconds();
						}
						// consultaForm.setTiempoDisponible(""+(date1.getHours()-date.getHours())+":"+date1.getMinutes()+":"+date1.getSeconds());
						consultaForm.setTiempoDisponible(hora + ":" + minutos
								+ ":" + segundos);
						consultaForm.setMostrarTiempoDisponible(true);
					}
				}

			}
		} else {
			consultaForm.setMostrarTrideTicket(false);
			consultaForm.setMostrarTiempoDisponible(false);
		}

		// validamos si es verificador de cuentas o prestamo
		List lineaTransaccionList = null;
		// cpalacios; 2016Octubre; valir si quitar todos estos datos de
		// VERIFICACION_CUENTAS o solamente el mostrarMsjError
		// if(consultaForm.getTransaccion().getTipoTransaccion().equals(TipoTransaccion.VERIFICACION_CUENTAS)
		// ||
		// consultaForm.getTransaccion().getTipoTransaccion().equals(TipoTransaccion.VERIFICACION_PRESTAMOS)){
		if (consultaForm.getTransaccion().getTipoTransaccion().equals(
				TipoTransaccion.VERIFICACION_PRESTAMOS)) {
			String filtroPlanilla = armarFiltroLineas(filtro);
			lineaTransaccionList = HibernateMap
					.queryByString(
							LineaTransaccion.class,
							("where transaccionHistorica = "
									+ consultaForm.getTransaccion().getId() + filtroPlanilla));
			consultaForm.setFiltroPlanilla(filtroPlanilla);
			boolean mostrarMsjError = false;

			consultaForm.getTransaccion().setCantidadCreditos(0);
			consultaForm.getTransaccion().setCantidadDebitos(0);
			consultaForm.getTransaccion().setMontoCreditos(0);
			consultaForm.getTransaccion().setMontoDebitos(0);
			for (Iterator iter = lineaTransaccionList.iterator(); iter
					.hasNext();) {
				LineaTransaccion linea = (LineaTransaccion) iter.next();
				if (linea.getReferencia() != null
						&& !"".equals(linea.getReferencia())
						&& !mostrarMsjError) {
					mostrarMsjError = true;
				}
				if (linea.esCredito()) {
					consultaForm.getTransaccion()
							.setCantidadCreditos(
									consultaForm.getTransaccion()
											.getCantidadCreditos() + 1);
					consultaForm.getTransaccion().setMontoCreditos(
							consultaForm.getTransaccion().getMontoCreditos()
									+ linea.getMonto());
				} else {
					consultaForm.getTransaccion()
							.setCantidadDebitos(
									consultaForm.getTransaccion()
											.getCantidadDebitos() + 1);
					consultaForm.getTransaccion().setMontoDebitos(
							consultaForm.getTransaccion().getMontoDebitos()
									+ linea.getMonto());
				}
			}
			if (mostrarMsjError) {
				consultaForm.setMostrarMsjError(true);
			} else {
				consultaForm.setMostrarMsjError(false);
			}
			consultaForm.setMostrarBotonGenerar(true);
		} else if (consultaForm.getTransaccion().getTipoTransaccion().equals(
				TipoTransaccion.VERIFICACION_CUENTAS)) {
			// inicio
			// codigo = 982 es cuenta mancomunada, pero solo es informativa
			// String filtroPlanilla = armarFiltroLineas(filtro);
			boolean mostrarbotonGenerar = true;

			lineaTransaccionList = HibernateMap.queryByString(
					LineaTransaccion.class,
					("where transaccionHistorica = " + consultaForm
							.getTransaccion().getId()));

			// cpalacios; verificar si se mantiene esta validacion o no;
			// OJOOOOOOOO
			if (lineaTransaccionList == null
					|| lineaTransaccionList.size() == 0) {
				List lineaTransaccionList2 = null;
				lineaTransaccionList2 = HibernateMap.queryByString(
						LineaTransaccion.class,
						("where transaccion = " + consultaForm.getTransaccion()
								.getId()));
				lineaTransaccionList = lineaTransaccionList2;
				mostrarbotonGenerar = false;
			}

			boolean mostrarMsjError = false;
			consultaForm.getTransaccion().setCantidadCreditos(0);
			consultaForm.getTransaccion().setCantidadDebitos(0);
			consultaForm.getTransaccion().setMontoCreditos(0);
			consultaForm.getTransaccion().setMontoDebitos(0);
			for (Iterator iter = lineaTransaccionList.iterator(); iter
					.hasNext();) {
				LineaTransaccion linea = (LineaTransaccion) iter.next();
				if (linea.getMensajeDevolucion() != 0
						&& linea.getResultado() != 0
						&& linea.getMensajeDevolucion() != 982
						&& !mostrarMsjError) {
					mostrarMsjError = true;
				}
				// v2
				if (linea.esCredito()) {
					consultaForm.getTransaccion()
							.setCantidadCreditos(
									consultaForm.getTransaccion()
											.getCantidadCreditos() + 1);
					consultaForm.getTransaccion().setMontoCreditos(
							consultaForm.getTransaccion().getMontoCreditos()
									+ linea.getMonto());
				} else {
					consultaForm.getTransaccion()
							.setCantidadDebitos(
									consultaForm.getTransaccion()
											.getCantidadDebitos() + 1);
					consultaForm.getTransaccion().setMontoDebitos(
							consultaForm.getTransaccion().getMontoDebitos()
									+ linea.getMonto());
				}
				// v2

			}
			if (mostrarMsjError) {
				consultaForm.setMostrarMsjError(true);
			} else {
				consultaForm.setMostrarMsjError(false);
			}
			// fin
			consultaForm.setMostrarBotonGenerar(mostrarbotonGenerar);
		} else {
			consultaForm.setMostrarBotonGenerar(false);
		}

		// trx ach en lote
		if (consultaForm.getTransaccion().getTipoTransaccion().equals(
				TipoTransaccion.PAGADURIAS_ACH)
				|| consultaForm.getTransaccion().getTipoTransaccion().equals(
						TipoTransaccion.PAGO_DE_PENSIONES_ACH)
				|| consultaForm.getTransaccion().getTipoTransaccion().equals(
						TipoTransaccion.PAGO_PLANILLA_ACH)
				|| consultaForm.getTransaccion().getTipoTransaccion().equals(
						TipoTransaccion.PAGO_PROVEEDORES_ACH)) {
			consultaForm.setMostrarDetalleACH(true);
			HashMap detalle2 = Servicios.obtenerDetalleACH(transaccion.getId());
			if (detalle2.isEmpty()) {
				consultaForm.setMostrarDetalleACH(false);
			} else {

				try {

					HashMap bancosList = Servicios.getMapBancosACH("planilEBE",
							"ebanca");
					HashMap detalleXBanco = new HashMap();
					HashMap totales = Servicios.totalesACH(transaccion.getId());
					detalleXBanco.clear();
					consultaForm.getResumenACH().clear();
					consultaForm.setMontoTotalACH(0.0);
					consultaForm.setTotalAbonos(0);
					Iterator entries = bancosList.entrySet().iterator();
					while (entries.hasNext()) {
						Map.Entry entry = (Map.Entry) entries.next();
						String key = (String) entry.getKey();
						DetalleBancoACH b = new DetalleBancoACH();
						b.setNombreBanco(key);
						b.setMontoTotal(0.0);
						b.setNumeroTransacciones(0);
						detalleXBanco.put(key, b);
					}
					detalleXBanco.putAll(detalle2);

					consultaForm.setTotalAbonos(Integer
							.parseInt((String) totales.get("totalAbonos")));
					consultaForm.setMontoTotalACH(Double
							.parseDouble((String) totales.get("montoTotal")));
					Iterator entries2 = detalleXBanco.entrySet().iterator();
					while (entries2.hasNext()) {
						Map.Entry entry = (Map.Entry) entries2.next();
						String key = (String) entry.getKey();
						DetalleBancoACH value = (DetalleBancoACH) entry
								.getValue();
						// System.out.println("Key = " + key + ", Value = " +
						// value);
						// System.out.println("Banco: "+value.getNombreBanco()+"
						// - "+"Abonos: "+value.getNumeroTransacciones()+" -
						// "+"Monto: "+value.getMontoTotal());
						consultaForm.getResumenACH().add(value);
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		// ----Guardamos un form de comprobantes
		ConsultaComprobantesForm formComprobante = new ConsultaComprobantesForm();
		formComprobante.setTransaccion(consultaForm.getTransaccion());
		formComprobante.setLinea(lineaDevolucion);
		// ----Guardamos el form de comprobantes
		session.setAttribute(SessionKeys.KEY_COMPROBANTES, formComprobante);
		session
				.setAttribute(SessionKeys.KEY_CONSULTA_TRANSACCION,
						consultaForm);
		return mapping.findForward("verdetalle");
	}

	/**
	 * Prepara el complemento del filtro para obtener las lineas de transacción,
	 * en base al filtro seleccionado por el usuario.
	 * 
	 * @param filtroLineas
	 * @return
	 */
	private String armarFiltroLineas(String filtroLineas) {
		String filtro = null;
		if (filtroLineas == null
				|| filtroLineas.trim().length() == 0
				|| filtroLineas.trim().equalsIgnoreCase(
						com.ba.potala.util.Constantes.FILTRO_TODAS)) {
			filtro = "";
		} else if (filtroLineas.trim().equalsIgnoreCase(
				com.ba.potala.util.Constantes.FILTRO_APLICADAS)) {
			filtro = " and resultado = 0";
		} else if (filtroLineas.trim().equalsIgnoreCase(
				com.ba.potala.util.Constantes.FILTRO_RECHAZADAS)) {
			filtro = " and resultado > 0";
		} else if (filtroLineas.trim().equalsIgnoreCase(
				com.ba.potala.util.Constantes.FILTRO_VALIDAS)) {
			filtro = " and referencia = \"\"";
		} else if (filtroLineas.trim().equalsIgnoreCase(
				com.ba.potala.util.Constantes.FILTRO_NO_VALIDAS)) {
			filtro = " and referencia != \"\"";
		} else {
			filtro = "";
		}
		return filtro;
	}

	/**
	 * 
	 * @param session
	 * @param consultaForm
	 * @param mapping
	 * @param parametros
	 * @return
	 * @throws Exception
	 */
	private ActionForward executeConsultaAuditoria(HttpSession session,
			ConsultaTransaccionForm consultaForm, ActionMapping mapping,
			ParametrosConsulta parametros) throws Exception {
		List list;
		Transaccion transaccion;
		Usuario usuario = getUsuario(session);

		// nos preocupamos por la salud del JVM
		{
			System.gc();
		}
		// transaccion = TransaccionUtils.loadTransaccion(usuario.getCliente(),
		// consultaForm.getIdTransaccion());
		String filtroLineas = armarFiltroLineas(Constantes.FILTRO_TODAS);
		transaccion = TransaccionUtils.loadTransaccionPaginada(usuario
				.getCliente(), consultaForm.getIdTransaccion(), consultaForm
				.getListaLineasTransaccion().getLinesXPage(), 0, filtroLineas,
				consultaForm.getFiltroUsuario());
		consultaForm.setTransaccion(transaccion);

		// ----Buscamos Tipo de Planilla para planillas de salarios
		String tipoPlanilla = "";
		if (transaccion.getCamposExtension() != null
				&& transaccion.getCamposExtension().size() > 0) {
			TrxEncExt campoEnc = null;
			for (int x = 0; x < transaccion.getCamposExtension().size(); x++) {
				campoEnc = (TrxEncExt) transaccion.getCamposExtension().get(x);
				if (campoEnc.getCampo().equals(
						Constantes.CAMPO_EXT_TIPO_PLANILLA)) {
					tipoPlanilla = campoEnc.getValor();
					break;
				}
			}
		}
		consultaForm.setTipoPlanilla(tipoPlanilla);

		// Obtenemos las estadisticas del impuesto si existe
		if (transaccion.getEstado().getAplicada()) {
			EstadisticaTransaccionImpuesto estadistica = TransaccionUtils
					.estadisticasCargosImpuestos(consultaForm
							.getIdTransaccion());
			consultaForm.setHayCargosImpuesto(estadistica != null);
			consultaForm.setEstadisticaImpuesto(estadistica);
		}
		// Determinamos si hay que mostrar mensaje de que aplica impuesto
		consultaForm.setMostrarMensajeImpuesto(aplicaImpuesto(transaccion));
		System.out
				.println("ConsultaTransaccionAction.executeConsultaAuditoria(), aplicaImpuesto = "
						+ consultaForm.getMostrarMensajeImpuesto());

		if (!parametros.getErrors().isEmpty()) {
			return mapping.findForward("error");
		} else {
			return mapping.findForward("verdetalle");
		}
	}

	/**
	 * Determina si a la transacción se le aplica impuesto, por medio del campo
	 * extensión de encabezado CAMPO_EXT_COBRO_IMPUESTO.
	 * 
	 * @param transaccion
	 * @return
	 */
	public boolean aplicaImpuesto(Transaccion transaccion) {
		boolean aplicaImpuesto = false;
		if (transaccion.getCamposExtension() != null
				&& transaccion.getCamposExtension().size() > 0) {
			TrxEncExt campoEnc = null;
			for (int x = 0; x < transaccion.getCamposExtension().size(); x++) {
				campoEnc = (TrxEncExt) transaccion.getCamposExtension().get(x);
				if (campoEnc.getCampo().equals(
						Constantes.CAMPO_EXT_COBRO_IMPUESTO)) {
					aplicaImpuesto = "Y".equals(campoEnc.getValor());
					break;
				}
			}
		}
		return aplicaImpuesto;
	}

	/**
	 * 
	 * @param session
	 * @param consultaForm
	 * @param mapping
	 * @return
	 * @throws Exception
	 */
	private ActionForward executeFiltrarLineas(HttpSession session,
			ConsultaTransaccionForm consultaForm, ActionMapping mapping)
			throws Exception {
		Usuario usuario = getUsuario(session);

		// ----Cargamos las lineas de la session
		List lineas = new ArrayList();
		// lineas.addAll(consultaForm.getTransaccion().getLineaTransaccionList());
		lineas.addAll(consultaForm.getListaLineasSinFiltrar());

		// ----filtramos por medio del parametro
		{
			if (!consultaForm.getFiltrarLineas().equals(
					ConsultaTransaccionForm.NO_FILTRAR)) {
				LineaDetalle lineaDetalle;
				if (consultaForm.getFiltrarLineas().equals(
						ConsultaTransaccionForm.FILTRAR_APLICADAS)) {
					for (int i = lineas.size() - 1; i >= 0; i--) {
						lineaDetalle = (LineaDetalle) lineas.get(i);
						if (lineaDetalle.getLinea().getResultado() != 0) {
							lineas.remove(i);
						}
					}
				} else {
					for (int i = lineas.size() - 1; i >= 0; i--) {
						lineaDetalle = (LineaDetalle) lineas.get(i);
						if (lineaDetalle.getLinea().getResultado() == 0) {
							lineas.remove(i);
						}
					}
				}
			}
		}

		// ----retornamos el resultado
		consultaForm.getListaLineasTransaccion().setRegistros(lineas);
		consultaForm.getListaLineasTransaccion().setCurrentPage(1);

		return mapping.findForward("verdetalle");
	}

	/**
	 * Filtra las lineas de detalle por el resultado de la operación de forma
	 * paginada
	 * 
	 * @param session
	 * @param consultaForm
	 * @param mapping
	 * @return
	 * @throws Exception
	 */
	private ActionForward executeFiltrarLineasPaginadas(HttpSession session,
			ConsultaTransaccionForm consultaForm, ActionMapping mapping,
			ParametrosConsulta parametros, int limit, int offset)
			throws Exception {
		Usuario usuario = getUsuario(session);

		// ----filtramos por medio del parametro
		{
			if (!consultaForm.getFiltrarLineas().equals(
					ConsultaTransaccionForm.NO_FILTRAR)) {
				if (consultaForm.getFiltrarLineas().equals(
						ConsultaTransaccionForm.FILTRAR_APLICADAS)) {
					executeConsultaDetalladaPaginada(session, consultaForm,
							mapping, parametros, limit, offset,
							Constantes.FILTRO_APLICADAS);
				} else if (consultaForm.getFiltrarLineas().equals(
						Constantes.FILTRO_RECHAZADAS)) {
					executeConsultaDetalladaPaginada(session, consultaForm,
							mapping, parametros, limit, offset,
							Constantes.FILTRO_RECHAZADAS);
				} else if (consultaForm.getFiltrarLineas().equals(
						Constantes.FILTRO_VALIDAS)) {
					executeConsultaDetalladaPaginada(session, consultaForm,
							mapping, parametros, limit, offset,
							Constantes.FILTRO_VALIDAS);
				} else
					executeConsultaDetalladaPaginada(session, consultaForm,
							mapping, parametros, limit, offset,
							Constantes.FILTRO_NO_VALIDAS);
			} else {
				executeConsultaDetalladaPaginada(session, consultaForm,
						mapping, parametros, limit, offset,
						Constantes.FILTRO_TODAS);
			}
		}

		// ----retornamos el resultado
		// consultaForm.getListaLineasTransaccion().setRegistros(lineas);
		consultaForm.getListaLineasTransaccion().setCurrentPage(1);

		return mapping.findForward("verdetalle");
	}

	/**
	 * Filtra las lineas de detalle por el resultado de la operación de
	 * validacion de cuentas o prestamos de forma paginada
	 * 
	 * @param session
	 * @param consultaForm
	 * @param mapping
	 * @return
	 * @throws Exception
	 */
	private ActionForward executeFiltrarLineasPaginadasValidas(
			HttpSession session, ConsultaTransaccionForm consultaForm,
			ActionMapping mapping, ParametrosConsulta parametros, int limit,
			int offset) throws Exception {
		Usuario usuario = getUsuario(session);

		// ----filtramos por medio del parametro
		{
			if (!consultaForm.getFiltrarLineas().equals(
					ConsultaTransaccionForm.NO_FILTRAR)) {
				if (consultaForm.getFiltrarLineas().equals(
						ConsultaTransaccionForm.FILTRAR_VALIDAS)) {
					executeConsultaDetalladaPaginada(session, consultaForm,
							mapping, parametros, limit, offset,
							Constantes.FILTRO_VALIDAS);
				} else {
					executeConsultaDetalladaPaginada(session, consultaForm,
							mapping, parametros, limit, offset,
							Constantes.FILTRO_NO_VALIDAS);
				}
			} else {
				executeConsultaDetalladaPaginada(session, consultaForm,
						mapping, parametros, limit, offset,
						Constantes.FILTRO_TODAS);
			}
		}

		// ----retornamos el resultado
		// consultaForm.getListaLineasTransaccion().setRegistros(lineas);
		consultaForm.getListaLineasTransaccion().setCurrentPage(1);

		return mapping.findForward("verdetalle");
	}

	/**
	 * 
	 * @param session
	 * @return
	 * @throws Exception
	 */
	private Map getCuentaReglasMap(HttpSession session, boolean force)
			throws Exception {
		Map map;
		Usuario usuario;

		usuario = getUsuario(session);
		StopWatch timer = new StopWatch();

		if (!force) {
			map = (Map) session
					.getAttribute(SessionKeys.KEY_CLIENTE_CUENTA_REGLA);
			if (map != null) {
				return map;
			}
		}

		{
			timer.start();
			map = AutorizacionesUtils.getClienteCuentaMap(usuario.getCliente());
			session.removeAttribute(SessionKeys.KEY_CLIENTE_CUENTA_REGLA);
			session.setAttribute(SessionKeys.KEY_CLIENTE_CUENTA_REGLA, map);
			timer.stop();
		}
		System.out.println("Duracion Trayendo Transacciones Pendientes para = "
				+ usuario.getUser() + " = " + timer);

		return map;
	}

	/**
	 * 
	 * @param session
	 * @return
	 * @throws Exception
	 */
	private Map getCuentaReglasMap(HttpSession session) throws Exception {
		return getCuentaReglasMap(session, false);
	}

	/**
	 * 
	 * @param session
	 * @param consultaForm
	 * @param mapping
	 * @param parametros
	 * @throws Exception
	 */
	private void executeAutorizarLote(HttpSession session,
			ConsultaTransaccionForm consultaForm, ActionMapping mapping,
			ParametrosConsulta parametros) throws Exception {
		Usuario usuario = getUsuario(session);
		Cliente cliente = usuario.getCliente();

		/** Nuevo bloque de validaciones * */
		boolean esUsuarioUnico = TransaccionUtils.usuarioUnico(usuario);
		boolean esTokenValido = false;
		boolean necesitaToken = false;

		/** Validamos que el token ingresado sea correcto* */
		String otp = consultaForm.getToken();
		String result = "token no generado";

		if (consultaForm.getMostrarCampoToken()) {
			/** Validamos token solamente si fue ingresado * */
			if (otp.length() > 0) {
				result = TokensUtils.validate(usuario.getUsuario().getToken(),
						otp);
				if (result != null) {
					esTokenValido = false;
					parametros
							.getErrors()
							.add(
									"autorizacion",
									new ActionError(
											"errors.detalletransaccion.notoken"));
				} else {
					if (result == null)
						esTokenValido = true;
				}

			}
		}

		ActionMessages mensajes = new ActionMessages();
		parametros.setMessages(mensajes);

		if (!usuario.compareClaveAutorizacion(consultaForm
				.getClaveAutorizacion())) {
			// ----Si la firma no coincide entonces devolvemos el mensaje
			parametros.getMessages().add("autorizacion",
					new ActionMessage("errors.detalletransaccion.nofirma"));
		} else {
			if (!usuario.tieneClaveAutorizacion()) {
				parametros.getErrors().add("autorizacion",
						new ActionError("errors.detalletransaccion.sinfirma"));
			} else {
				Number secuencial;
				Transaccion transaccion;
				int cantidadPorToken = 0;
				int cantidadAutorizadas = 0;
				int cantidadPendienteFirma = 0;
				int cantidadTransaccionesAutorizar = consultaForm
						.getListaAutorizar().length;
				Map map = getCuentaReglasMap(session);

				// INICIO: cpalacios; Dic2013/Ene2014; fechas de corte ISSS/AFP;
				// variables inicializadas
				int cuentaTipoPagoISSS = 0;
				int cuentaTipoPagoAFP = 0;
				int cuentaTipoPagoIPSFA = 0;
				// FIN: cpalacios; Dic2013/Ene2014; fechas de corte ISSS/AFP;

				for (int i = 0; i < consultaForm.getListaAutorizar().length; i++) {
					secuencial = new Long(consultaForm.getListaAutorizar()[i]);
					transaccion = TransaccionUtils.loadTransaccion(cliente,
							secuencial.longValue());

					if (transaccion.getEstado().equals(
							EstadoTransaccion.ESTADO_PENDIENTE_AUTORIZACION)) {

						/**
						 * Si es transacción por token hacemos la validacion
						 * respectiva *
						 */
						if ((esUsuarioUnico)
								&& (TransaccionUtils
										.esTransaccionPorToken(transaccion
												.getTipoTransaccion())))
							necesitaToken = true;
						else
							necesitaToken = false;

						// INICIO: cpalacios; Dic2013/Ene2014; fechas de corte
						// ISSS/AFP;
						boolean banderaRechazoPago = true;
						int valorRetornoFechaLimites = 0;
						String valorRetornoFechaLimitesStr = "";
						if (transaccion.getTipoTransaccion().getDescripcion()
								.equals(
										TipoTransaccion.PAGO_AFP
												.getDescripcion())) {
							// obtener el codigo de tipo de Afp de linea de
							// transaccion
							int tipoCompaniaPago = 0;
							tipoCompaniaPago = ServiciosTransaccionesYConsultas
									.codigoAfpCorte(transaccion);
							// periodo de pago de afp de linea de transaccion
							String periodoPagoAfp = "";
							periodoPagoAfp = ServiciosTransaccionesYConsultas
									.periodoPagoAfpCorte(transaccion);
							System.out
									.println("####PERIODO AFP PAGAR EN TRANSACCION: "
											+ periodoPagoAfp);

							// validacion de tipo: AFP, IPSFA o ISSS

							valorRetornoFechaLimitesStr = ServiciosTransaccionesYConsultas
									.fechaCortePago_AFP_lote_consultas(
											tipoCompaniaPago, 2, periodoPagoAfp);

							String[] varTemp = new String[2];
							varTemp = valorRetornoFechaLimitesStr.split(",");

							String nombreCompania = "AFP/IPSFA";
							if (tipoCompaniaPago == 4) {
								nombreCompania = "IPSFA";
								cuentaTipoPagoIPSFA = cuentaTipoPagoIPSFA + 1;
							} else {
								nombreCompania = "AFP";
								cuentaTipoPagoAFP = cuentaTipoPagoAFP + 1;
							}

							valorRetornoFechaLimites = Integer
									.parseInt(varTemp[0]);

							// validacion de cantidad de tipo AFP, IPSFA, ISSS
							if (valorRetornoFechaLimites <= -1)
								banderaRechazoPago = false;

							// if (valorRetornoFechaLimites = -1)
							// cuentaTipoPagoAFP = cuentaTipoPagoAFP + 1;

							if (cuentaTipoPagoAFP == 1) {
								switch (valorRetornoFechaLimites) {
								case -3:
									parametros
											.getErrors()
											.add(
													"pagoafp",
													new ActionError(
															"mensaje.simple",
															"ERROR DE COMUNICACION, CONSULTE CON EL ADIMINISTRADOR"));
									break;
								case -2:
									parametros
											.getErrors()
											.add(
													"pagoafp",
													new ActionError(
															"mensaje.simple",
															"ERROR DE FECHA CONFIGURADA/TIPO DE AFP, ETC. CONSULTE CON EL ADIMINISTRADOR"));
									break;
								case -1:
									// Lleva de parametro el nombre del tipo de
									// Pago
									parametros
											.getErrors()
											.add(
													"pagoafp",
													new ActionError(
															"lp.mensaje.fechalimiteAutorizar4",
															nombreCompania,
															"autorizada y aplicada"));
									// parametros.getMessages().add("pagoafp",
									// new
									// ActionError("lp.mensaje.fechalimiteAutorizar2",nombreCompaniaPagar,tipoPaso));
									break;
								case 0:
									// No mostrar ningun mensaje
									break;
								case 1:
									// Lleva de parametro la fecha limite y la
									// hora limite de pago
									// parametros.getMessages().add("pagoafp",
									// new
									// ActionError("lp.mensaje.fechalimiteAutorizar1","autorizada",nombreCompania,varTemp[1]));
									parametros
											.getMessages()
											.add(
													"pagoafp",
													new ActionError(
															"lp.mensaje.fechalimiteAutorizar3",
															nombreCompania,
															"autorizada y aplicada",
															varTemp[1]));
								}
								cuentaTipoPagoAFP = cuentaTipoPagoAFP + 1;
							}

							if (cuentaTipoPagoIPSFA == 1) {
								switch (valorRetornoFechaLimites) {
								case -3:
									parametros
											.getErrors()
											.add(
													"pagoafp",
													new ActionError(
															"mensaje.simple",
															"ERROR DE COMUNICACION, CONSULTE CON EL ADIMINISTRADOR"));
									break;
								case -2:
									parametros
											.getErrors()
											.add(
													"pagoafp",
													new ActionError(
															"mensaje.simple",
															"ERROR DE FECHA CONFIGURADA/TIPO DE AFP, ETC. CONSULTE CON EL ADIMINISTRADOR"));
									break;
								case -1:
									// Lleva de parametro el nombre del tipo de
									// Pago
									parametros
											.getErrors()
											.add(
													"pagoafp",
													new ActionError(
															"lp.mensaje.fechalimiteAutorizar4",
															nombreCompania,
															"autorizada y aplicada"));
									// parametros.getMessages().add("pagoafp",
									// new
									// ActionError("lp.mensaje.fechalimiteAutorizar2",nombreCompaniaPagar,tipoPaso));
									break;
								case 0:
									// No mostrar ningun mensaje
									break;
								case 1:
									// Lleva de parametro la fecha limite y la
									// hora limite de pago
									// parametros.getMessages().add("pagoafp",
									// new
									// ActionError("lp.mensaje.fechalimiteAutorizar1","autorizada",nombreCompania,varTemp[1]));
									parametros
											.getMessages()
											.add(
													"pagoafp",
													new ActionError(
															"lp.mensaje.fechalimiteAutorizar3",
															nombreCompania,
															"autorizada y aplicada",
															varTemp[1]));
								}
								cuentaTipoPagoIPSFA = cuentaTipoPagoIPSFA + 1;

							}

							// if (!parametros.getErrors().isEmpty() &&
							// valorRetornoFechaLimites == -1) {
							if (!parametros.getErrors().isEmpty()
									&& valorRetornoFechaLimites <= -1) {
								transaccion
										.setEstado(EstadoTransaccion.ESTADO_RECHAZADA);
								HibernateMap.update(transaccion);
								TransaccionUtils
										.pasarTransaccionAHistorica_v2(transaccion);
							}
						} else if (transaccion.getTipoTransaccion()
								.getDescripcion().equals(
										TipoTransaccion.PAGO_ISSS
												.getDescripcion())
								|| transaccion
										.getTipoTransaccion()
										.getDescripcion()
										.equals(
												TipoTransaccion.PAGO_PLANILLA_ISSS_EDUCO
														.getDescripcion())) {
							valorRetornoFechaLimitesStr = ServiciosTransaccionesYConsultas
									.fechaCortePago_ISSS_lote_consultas(55);

							String[] varTemp = new String[2];
							varTemp = valorRetornoFechaLimitesStr.split(",");

							String nombreCompania = "ISSS";
							valorRetornoFechaLimites = Integer
									.parseInt(varTemp[0]);

							// validacion de cantidad de tipo AFP, IPSFA, ISSS
							if (valorRetornoFechaLimites <= -1)
								banderaRechazoPago = false;

							// if (valorRetornoFechaLimites = -1)
							cuentaTipoPagoISSS = cuentaTipoPagoISSS + 1;

							if (cuentaTipoPagoISSS == 1) {
								switch (valorRetornoFechaLimites) {
								case -3:
									parametros
											.getErrors()
											.add(
													"pagoafp",
													new ActionError(
															"mensaje.simple",
															"ERROR DE COMUNICACION, CONSULTE CON EL ADIMINISTRADOR"));
									break;
								case -2:
									parametros
											.getErrors()
											.add(
													"pagoafp",
													new ActionError(
															"mensaje.simple",
															"ERROR DE FECHA CONFIGURADA/TIPO DE AFP, ETC. CONSULTE CON EL ADIMINISTRADOR"));
									break;
								case -1:
									// Lleva de parametro el nombre del tipo de
									// Pago
									parametros
											.getErrors()
											.add(
													"pagoafp",
													new ActionError(
															"lp.mensaje.fechalimiteAutorizar4",
															nombreCompania,
															"autorizada y aplicada"));
									break;
								case 0:
									// No mostrar ningun mensaje
									break;
								case 1:
									// Lleva de parametro la fecha limite y la
									// hora limite de pago
									parametros
											.getMessages()
											.add(
													"pagoafp",
													new ActionError(
															"lp.mensaje.fechalimiteAutorizar3",
															nombreCompania,
															"autorizada y aplicada",
															varTemp[1]));
								}
							}

							if (!parametros.getErrors().isEmpty()
									&& valorRetornoFechaLimites <= -1) {
								transaccion
										.setEstado(EstadoTransaccion.ESTADO_RECHAZADA);
								HibernateMap.update(transaccion);
								TransaccionUtils
										.pasarTransaccionAHistorica_v2(transaccion);
							}
						}
						if (banderaRechazoPago == true) {
							// cpalacios; Dic2013; fechas de corte ISSS/AFP; se
							// agrega if para que solo ingrese el update cuando
							// no realice cambio por rechazo de la transaccion
							// FIN: cpalacios; Dic2013/Ene2014; fechas de corte
							// ISSS/AFP;

							/**
							 * Doble validacion: Token necesario y token
							 * correctamente ingresado *
							 */
							if (necesitaToken) {
								cantidadPorToken++;
								if (esTokenValido) {
									// ---- Si faltan firmas entonces hacemos la
									// autorizacion
									AutorizacionesUtils.addAutorizacion(
											transaccion, usuario, map);
									if (AutorizacionesHelper
											.transaccionCompletaTodasAutorizaciones(
													transaccion, map)) {
										cantidadAutorizadas++;
										if (transaccion
												.getFechaProgramacionAutomatica() > 0) {
											transaccion
													.setEstado(EstadoTransaccion.ESTADO_PROGRAMADA_POR_APLICAR);
										} else {
											transaccion
													.setEstado(EstadoTransaccion.ESTADO_PENDIENTE_APLICACION);
										}
										HibernateMap.update(transaccion);
									} else {
										cantidadAutorizadas++;
										cantidadPendienteFirma++;
									}
								}
							} else {
								// ---- Si faltan firmas entonces hacemos la
								// autorizacion
								AutorizacionesUtils.addAutorizacion(
										transaccion, usuario, map);
								if (AutorizacionesHelper
										.transaccionCompletaTodasAutorizaciones(
												transaccion, map)) {
									cantidadAutorizadas++;

									if (transaccion
											.getFechaProgramacionAutomatica() > 0) {
										transaccion
												.setEstado(EstadoTransaccion.ESTADO_PROGRAMADA_POR_APLICAR);
									} else {
										System.out
												.println("ConsultaTransaccionAction.executeAutorizarLote: Cambiando estado de transaccion "
														+ transaccion.getId()
														+ " a Pendiente de Aplicacion");
										transaccion
												.setEstado(EstadoTransaccion.ESTADO_PENDIENTE_APLICACION);
									}
									HibernateMap.update(transaccion);
								} else {
									cantidadAutorizadas++;
									cantidadPendienteFirma++;
								}
							}

						}// cpalacios; fin de ELSE; dic2013; fechas de corte
							// ISSS/AFP
					} else {
						if (transaccion.getEstado().equals(
								EstadoTransaccion.ESTADO_APLICADA)
								|| transaccion.getEstado().equals(
										EstadoTransaccion.ESTADO_RECHAZADA)
								|| transaccion.getEstado().equals(
										EstadoTransaccion.ESTADO_APLICANDO)) {
							// La transaccion ya esta aplicada o esta siendo
							// aplicada
							parametros
									.getErrors()
									.add(
											"autorizacion",
											new ActionError(
													"errors.detalletransaccion.yaprocesada"));
						} else {
							if (transaccion
									.getEstado()
									.equals(
											EstadoTransaccion.ESTADO_PENDIENTE_APLICACION)) {
								// La transaccion ya esta autorizada, solo esta
								// pendiente de aplicar
								parametros
										.getErrors()
										.add(
												"autorizacion",
												new ActionError(
														"errors.detalletransaccion.yaautorizada"));
							} else {
								// cualquier otro estado diferente a pendiente
								// de autorizar
								parametros
										.getErrors()
										.add(
												"autorizacion",
												new ActionError(
														"errors.detalletransaccion.noautorizada"));
								parametros
										.getErrors()
										.add(
												"autorizacion",
												new ActionError(
														"errors.detalletransaccion.noestado"));
							}
						}
					}
				}

				// forma el mensaje al usuario
				{
					String str = "";
					String strAux = "";

					if (cantidadPorToken == 1) {
						strAux = "Existe una transacción que requiere del código de activación !";
					}
					if (cantidadPorToken > 1)
						strAux = "Existen "
								+ cantidadPorToken
								+ " transacciones que requieren del código de activación !";

					if (cantidadAutorizadas == 1) {
						str += "Una transaccion firmada";
						if (cantidadPendienteFirma > 0) {
							str += (cantidadPendienteFirma == 1) ? (" que requiere de otra autorizacion!")
									: (", quedan " + cantidadPendienteFirma + " pendientes de autorizacion!");
						} else {
							str += " pendiente de aplicar!";
						}
					} else if (cantidadAutorizadas > 1) {
						str += cantidadAutorizadas + " transacciones firmadas";
						if (cantidadPendienteFirma == 0) {
							str += " pendientes de aplicar!";
						} else if (cantidadPendienteFirma == 1) {
							str += ", queda una que requiere otra autorizacion!";
						} else if (cantidadPendienteFirma == cantidadAutorizadas) {
							str += " las cuales estan pendientes de otra autorizacion!";
						} else {
							str += ", quedan " + cantidadPendienteFirma
									+ " que requieren de otra autorizacion!";
						}
					} else {
						str += "Ninguna transaccion fue firmada!";
					}

					parametros.getMessages().add(
							"autorizacion",
							new ActionMessage("mensaje.simple.individual", str
									+ "<br>"));

					/**
					 * Si son transacciones que requieren codigo de activación
					 * solamente se presentara este mensaje informativo *
					 */
					if (strAux.length() > 0
							&& cantidadPorToken > 0
							&& cantidadTransaccionesAutorizar > cantidadAutorizadas)
						parametros.getMessages().add(
								"autorizacion",
								new ActionMessage("mensaje.simple.individual",
										strAux + "<br>"));
				}
			}
		}
		// Seatemos la lista de transacciones por autorizar
		consultaForm.setListaAutorizar(new String[0]);
	}

	/**
	 * 
	 * @param session
	 * @param consultaForm
	 * @param mapping
	 * @param parametros
	 * @throws Exception
	 */
	private void executeAplicarLote(HttpSession session,
			ConsultaTransaccionForm consultaForm, ActionMapping mapping,
			ParametrosConsulta parametros, HttpServletRequest request)
			throws Exception {
		StringBuffer sbAplicadas = new StringBuffer(" ");
		StringBuffer sbNoAplicadas = new StringBuffer(" ");
		StringBuffer sbRechazadas = new StringBuffer(" ");
		StringBuffer sbAplicandoACH = new StringBuffer(" ");
		StringBuffer sbAplicando = new StringBuffer(" ");
		Number secuencial;
		int aplicadas = 0;
		int rechazadas = 0;
		int aplicandoACH = 0;
		int aplicando = 0;
		int programadas = 0;
		Transaccion transaccion;
		ActionMessages mensajes = new ActionMessages();

		Usuario usuario = getUsuario(session);
		Cliente cliente = usuario.getCliente();

		// takes care of JVM
		{
			System.gc();
		}

		// INICIO: cpalacios; Dic2013/Ene2014; fechas de corte ISSS/AFP;
		// variables inicializadas
		int cuentaTipoPagoISSS = 0;
		int cuentaTipoPagoAFP = 0;
		int cuentaTipoPagoIPSFA = 0;
		// FIN: cpalacios; Dic2013/Ene2014; fechas de corte ISSS/AFP;

		parametros.setMessages(mensajes);
		for (int i = 0; i < consultaForm.getListaAutorizar().length; i++) {
			secuencial = new Long(consultaForm.getListaAutorizar()[i]);
			transaccion = TransaccionUtils.loadTransaccion(cliente, secuencial
					.longValue());

			// cpalacios; 20160628; auditoriaUsuarios; se agrega ip y se guarda
			// en base de datos; este if se agrega para limitar solo estas
			// transacciones
			// se agrega parametro request en los parametros del metodo
			// "executeAplicarLote"
			if (transaccion.getTipoTransaccion().getDescripcion().trim()
					.equals(TipoTransaccion.DATOS_USUARIO.getDescripcion())
					|| transaccion
							.getTipoTransaccion()
							.getDescripcion()
							.trim()
							.equals(
									TipoTransaccion.SOLICITUD_AFILIACION_TRX_AUTOMATICA
											.getDescripcion())) {
				String ip = "";
				ip = request.getRemoteAddr() != null ? request.getRemoteAddr()
						: " ";
				System.out.println("ip remota de cambio datos: " + ip);

				transaccion.setIpRemota(ip);

				System.out.println("guardarIpRemota: " + ip);
				// transaccion.setDescripcion(ip);
				transaccion.setMemoAsBytes(Utils.writeObject(ip));

				HibernateMap.update(transaccion);
			}

			// ----Validamos el estado y que la transaccion no sea programada
			if (transaccion.getEstado().equals(
					EstadoTransaccion.ESTADO_PENDIENTE_APLICACION)) {
				// INICIO: cpalacios; Dic2013/Ene2014; fechas de corte ISSS/AFP;
				boolean banderaRechazoPago = true;
				int valorRetornoFechaLimites = 0;
				String valorRetornoFechaLimitesStr = "";

				if (transaccion.getTipoTransaccion().getDescripcion().equals(
						TipoTransaccion.PAGO_AFP.getDescripcion())) {
					// obtener el codigo de tipo de Afp de linea de transaccion
					int tipoCompaniaPago = 0;
					tipoCompaniaPago = ServiciosTransaccionesYConsultas
							.codigoAfpCorte(transaccion);
					// periodo de pago de afp de linea de transaccion
					String periodoPagoAfp = "";
					periodoPagoAfp = ServiciosTransaccionesYConsultas
							.periodoPagoAfpCorte(transaccion);
					System.out.println("####PERIODO AFP PAGAR EN TRANSACCION: "
							+ periodoPagoAfp);

					valorRetornoFechaLimitesStr = ServiciosTransaccionesYConsultas
							.fechaCortePago_AFP_lote_consultas(
									tipoCompaniaPago, 3, periodoPagoAfp);

					String[] varTemp = new String[2];
					varTemp = valorRetornoFechaLimitesStr.split(",");

					String nombreCompania = "AFP/IPSFA";
					if (tipoCompaniaPago == 4) {
						nombreCompania = "IPSFA";
						cuentaTipoPagoIPSFA = cuentaTipoPagoIPSFA + 1;
					} else {
						nombreCompania = "AFP";
						cuentaTipoPagoAFP = cuentaTipoPagoAFP + 1;
					}

					valorRetornoFechaLimites = Integer.parseInt(varTemp[0]);

					// validacion de cantidad de tipo AFP, IPSFA, ISSS
					if (valorRetornoFechaLimites <= -1)
						banderaRechazoPago = false;

					// cuentaTipoPagoAFP = cuentaTipoPagoAFP + 1;

					if (cuentaTipoPagoAFP == 1) {
						switch (valorRetornoFechaLimites) {
						case -3:
							parametros
									.getErrors()
									.add(
											"pagoafp",
											new ActionError("mensaje.simple",
													"ERROR DE COMUNICACION, CONSULTE CON EL ADIMINISTRADOR"));
							break;
						case -2:
							parametros
									.getErrors()
									.add(
											"pagoafp",
											new ActionError("mensaje.simple",
													"ERROR DE FECHA CONFIGURADA/TIPO DE AFP, ETC. CONSULTE CON EL ADIMINISTRADOR"));
							break;
						case -1:
							// Lleva de parametro el nombre del tipo de Pago
							parametros.getErrors().add(
									"pagoafp",
									new ActionError(
											"lp.mensaje.fechalimiteAutorizar4",
											nombreCompania, "aplicada"));
							break;
						case 0:
							// No mostrar ningun mensaje
							break;
						case 1:
							parametros.getMessages().add(
									"pagoafp",
									new ActionError(
											"lp.mensaje.fechalimiteAutorizar3",
											nombreCompania, "aplicada",
											varTemp[1]));
						}
						cuentaTipoPagoAFP = cuentaTipoPagoAFP + 1;

					}

					if (cuentaTipoPagoIPSFA == 1) {
						switch (valorRetornoFechaLimites) {
						case -3:
							parametros
									.getErrors()
									.add(
											"pagoafp",
											new ActionError("mensaje.simple",
													"ERROR DE COMUNICACION, CONSULTE CON EL ADIMINISTRADOR"));
							break;
						case -2:
							parametros
									.getErrors()
									.add(
											"pagoafp",
											new ActionError("mensaje.simple",
													"ERROR DE FECHA CONFIGURADA/TIPO DE AFP, ETC. CONSULTE CON EL ADIMINISTRADOR"));
							break;
						case -1:
							// Lleva de parametro el nombre del tipo de Pago
							parametros.getErrors().add(
									"pagoafp",
									new ActionError(
											"lp.mensaje.fechalimiteAutorizar4",
											nombreCompania, "aplicada"));
							break;
						case 0:
							// No mostrar ningun mensaje
							break;
						case 1:
							parametros.getMessages().add(
									"pagoafp",
									new ActionError(
											"lp.mensaje.fechalimiteAutorizar3",
											nombreCompania, "aplicada",
											varTemp[1]));
						}
						cuentaTipoPagoIPSFA = cuentaTipoPagoIPSFA + 1;
					}

					// if (!parametros.getErrors().isEmpty() &&
					// valorRetornoFechaLimites == -1) {
					if (!parametros.getErrors().isEmpty()
							&& valorRetornoFechaLimites <= -1) {
						transaccion
								.setEstado(EstadoTransaccion.ESTADO_RECHAZADA);
						HibernateMap.update(transaccion);
						TransaccionUtils
								.pasarTransaccionAHistorica_v2(transaccion);
					}
				} else if (transaccion.getTipoTransaccion().getDescripcion()
						.equals(TipoTransaccion.PAGO_ISSS.getDescripcion())
						|| transaccion
								.getTipoTransaccion()
								.getDescripcion()
								.equals(
										TipoTransaccion.PAGO_PLANILLA_ISSS_EDUCO
												.getDescripcion())) {
					valorRetornoFechaLimitesStr = ServiciosTransaccionesYConsultas
							.fechaCortePago_ISSS_lote_consultas(55);
					String[] varTemp = new String[2];
					varTemp = valorRetornoFechaLimitesStr.split(",");

					String nombreCompania = "ISSS";
					valorRetornoFechaLimites = Integer.parseInt(varTemp[0]);

					// validacion de cantidad de tipo AFP, IPSFA, ISSS
					if (valorRetornoFechaLimites <= -1)
						banderaRechazoPago = false;

					// if (valorRetornoFechaLimites = -1)
					cuentaTipoPagoISSS = cuentaTipoPagoISSS + 1;

					if (cuentaTipoPagoISSS == 1) {
						switch (valorRetornoFechaLimites) {
						case -3:
							parametros
									.getErrors()
									.add(
											"pagoafp",
											new ActionError("mensaje.simple",
													"ERROR DE COMUNICACION, CONSULTE CON EL ADIMINISTRADOR"));
							break;
						case -2:
							parametros
									.getErrors()
									.add(
											"pagoafp",
											new ActionError("mensaje.simple",
													"ERROR DE FECHA CONFIGURADA/TIPO DE AFP, ETC. CONSULTE CON EL ADIMINISTRADOR"));
							break;
						case -1:
							// Lleva de parametro el nombre del tipo de Pago
							parametros.getErrors().add(
									"pagoafp",
									new ActionError(
											"lp.mensaje.fechalimiteAutorizar4",
											nombreCompania, "aplicada"));
							break;
						case 0:
							// No mostrar ningun mensaje
							break;
						case 1:
							parametros.getMessages().add(
									"pagoafp",
									new ActionError(
											"lp.mensaje.fechalimiteAutorizar3",
											nombreCompania, "aplicada",
											varTemp[1]));
						}
					}

					if (!parametros.getErrors().isEmpty()
							&& valorRetornoFechaLimites <= -1) {
						transaccion
								.setEstado(EstadoTransaccion.ESTADO_RECHAZADA);
						HibernateMap.update(transaccion);
						TransaccionUtils
								.pasarTransaccionAHistorica_v2(transaccion);
					}
				}
				// FIN: cpalacios; Dic2013/Ene2014; fechas de corte ISSS/AFP;
				if (banderaRechazoPago == true) {
					// cpalacios; Dic2013; fechas de corte ISSS/AFP; se agrega
					// if para que solo ingrese el update cuando no realice
					// cambio por rechazo de la transaccion
					if (transaccion.getTransaccionCuentaCargoOk()) {
						if (validaDisponibilidadServicios(transaccion)) {
							TransactionDispacher.execute(transaccion, usuario);
							if (transaccion.getEstado() == EstadoTransaccion.ESTADO_APLICADA) {
								aplicadas++;
							} else if (transaccion.getEstado() == EstadoTransaccion.ESTADO_RECHAZADA) {
								rechazadas++;
								sbRechazadas.append("-ID "
										+ transaccion.getSecuencial()
										+ "-"
										+ transaccion.getTipoTransaccion()
												.getDescripcion() + "<br>");
							} else if (transaccion.getEstado() == EstadoTransaccion.ESTADO_APLICANDO) {
								aplicando++;
								sbAplicando.append("-ID "
										+ transaccion.getSecuencial()
										+ "-"
										+ transaccion.getTipoTransaccion()
												.getDescripcion() + "<br>");
							} else if (transaccion.getEstado() == EstadoTransaccion.ESTADO_ENVIADO_ACH) {
								aplicandoACH++;
								sbAplicandoACH.append("-ID "
										+ transaccion.getSecuencial()
										+ "-"
										+ transaccion.getTipoTransaccion()
												.getDescripcion() + "<br>");
							}
						} else {
							parametros.getErrors().add(
									"autorizacion",
									new ActionError(
											"errors.transaccion.reintentar"));
						}
						// Se realiza delay para evitar realizar otra
						// transacción, cuando todavía no se ha actualizado el
						// saldo
						if (!TransactionDispacher.esMultiple(transaccion)) {
							Utils.sleepQuietly(3000);
						}
					}
				}// fin del if(banderaRechazoPago == true) para controlar que
					// si no se ingresa de ningun tipo de pago ISSS o AFP
			} else if (transaccion.getEstado().equals(
					EstadoTransaccion.ESTADO_PROGRAMADA_POR_APLICAR)) {
				programadas++;
			}
		}

		if (aplicadas > 0) {
			sbAplicadas.append(aplicadas);
			parametros.getMessages().add(
					"autorizacion",
					new ActionMessage("mensajes.detalletransaccion.aplicada",
							sbAplicadas.toString()));
		}
		if (rechazadas > 0) {
			parametros.getMessages().add(
					"autorizacion",
					new ActionMessage("mensajes.detalletransaccion.rechazada",
							rechazadas + ""));
			parametros.getMessages().add(
					"autorizacion",
					new ActionMessage("errors.generico", sbRechazadas
							.toString()));
		}
		if (aplicando > 0) {
			parametros.getMessages().add(
					"autorizacion",
					new ActionMessage("mensajes.detalletransaccion.aplicando",
							aplicando + ""));
			parametros.getMessages()
					.add(
							"autorizacion",
							new ActionMessage("errors.generico", sbAplicando
									.toString()));
		}
		if (aplicandoACH > 0) {
			parametros.getMessages().add(
					"autorizacion",
					new ActionMessage(
							"mensajes.detalletransaccion.aplicandoACH",
							aplicandoACH + ""));
			parametros.getMessages().add(
					"autorizacion",
					new ActionMessage("errors.generico", sbAplicandoACH
							.toString()));
		}
		if (programadas > 0) {
			sbNoAplicadas.append(programadas);
			parametros
					.getMessages()
					.add(
							"autorizacion",
							new ActionMessage(
									"mensajes.detalletransaccion.programada.noaplicada",
									sbNoAplicadas.toString()));
		}

	}

	/**
	 * 
	 * @param session
	 * @param consultaForm
	 * @param mapping
	 * @param parametros
	 * @throws Exception
	 */
	private void executeAutorizar(HttpSession session,
			ConsultaTransaccionForm consultaForm, ActionMapping mapping,
			ParametrosConsulta parametros) throws Exception {
		Usuario usuario = getUsuario(session);
		ActionMessages mensajes = new ActionMessages();
		parametros.setMessages(mensajes);
		if (!usuario.compareClaveAutorizacion(consultaForm
				.getClaveAutorizacion())) {
			// ----Si la firma no coincide entonces devolvemos el mensaje
			parametros.getMessages().add("autorizacion",
					new ActionMessage("errors.detalletransaccion.nofirma"));

			// ----Volvemos hacer la consulta detallada de la misma transaccion
			consultaForm
					.setIdTransaccion(consultaForm.getTransaccion().getId());
			parametros.setForward(executeConsultaDetallada(session,
					consultaForm, mapping, parametros));
			return;
		}

		/** Validaciones de token * */
		if (consultaForm.getMostrarCampoToken()) {
			System.out.println("validacion campo token");

			/**
			 * Validamos que el usuario tenga el token para que pueda autorizar
			 * la transaccion *
			 */
			if (!usuario.getTieneToken()) {
				parametros.getMessages()
						.add(
								"autorizacion",
								new ActionMessage(
										"errors.detalletransaccion.sintoken"));
				// ----Volvemos hacer la consulta detallada de la misma
				// transaccion
				consultaForm.setIdTransaccion(consultaForm.getTransaccion()
						.getId());
				parametros.setForward(executeConsultaDetallada(session,
						consultaForm, mapping, parametros));
				return;
			}

			/** Validamos que el token ingresado sea correcto* */
			String otp = consultaForm.getToken();
			String result = "token no generado";
			if (otp.length() > 0) {
				result = TokensUtils.validate(usuario.getUsuario().getToken(),
						otp);
			}
			if (result != null) {
				parametros.getMessages().add("autorizacion",
						new ActionMessage("errors.detalletransaccion.notoken"));
				// ----Volvemos hacer la consulta detallada de la misma
				// transaccion
				consultaForm.setIdTransaccion(consultaForm.getTransaccion()
						.getId());
				parametros.setForward(executeConsultaDetallada(session,
						consultaForm, mapping, parametros));
				return;
			}
		}

		if (!usuario.tieneClaveAutorizacion()) {
			parametros.getErrors().add("autorizacion",
					new ActionError("errors.detalletransaccion.sinfirma"));
		} else {
			if (consultaForm.getTransaccion().getEstado().equals(
					EstadoTransaccion.ESTADO_PENDIENTE_AUTORIZACION)) {
				// ----Si faltan autorizaciones entonces hacemos la autorizacion
				// modificacion: cpalacios; Dic2013; fechas de corte ISSS/AFP;
				// se agrega variable banderaRechazoPago
				boolean banderaRechazoPago = true;
				Transaccion transaccion = consultaForm.getTransaccion();
				Map map = getCuentaReglasMap(session);
				AutorizacionesUtils.addAutorizacion(transaccion, usuario, map);
				if (AutorizacionesHelper
						.transaccionCompletaTodasAutorizaciones(transaccion,
								map)) {
					// parametros.getMessages().add("autorizacion", new
					// ActionMessage("mensajes.detalletransaccion.autorizada.completa"));
					if (transaccion.getFechaProgramacionAutomatica() > 0) {
						transaccion
								.setEstado(EstadoTransaccion.ESTADO_PROGRAMADA_POR_APLICAR);
					} else {
						// cpalacios
						if (consultaForm.getTransaccion().getTipoTransaccion()
								.getDescripcion().trim().equals(
										TipoTransaccion.PAGO_AFP
												.getDescripcion())) {
							// obtener el codigo de tipo de Afp de linea de
							// transaccion
							int tipoCompaniaPago = 0;
							tipoCompaniaPago = ServiciosTransaccionesYConsultas
									.codigoAfpCorte(transaccion);
							// obtener el periodo de Afp de linea de transaccion
							String periodoPagoAfp = "";
							periodoPagoAfp = ServiciosTransaccionesYConsultas
									.periodoPagoAfpCorte(transaccion);
							System.out
									.println("####PERIODO AFP PAGAR EN TRANSACCION: "
											+ periodoPagoAfp);

							int pasoTransaccion = 3;

							banderaRechazoPago = ServiciosTransaccionesYConsultas
									.fechaCortePago_AFP_consultas(
											tipoCompaniaPago, mapping,
											parametros, 1, pasoTransaccion,
											periodoPagoAfp);
							if (!parametros.getErrors().isEmpty()) {
								transaccion
										.setEstado(EstadoTransaccion.ESTADO_RECHAZADA);
								HibernateMap.update(transaccion);
								TransaccionUtils
										.pasarTransaccionAHistorica_v2(transaccion);
								parametros
										.getMessages()
										.add(
												"autorizacion",
												new ActionMessage(
														"mensaje.simple",
														"La Transacci&oacute;n ha sido rechazada"));
							} else
								transaccion
										.setEstado(EstadoTransaccion.ESTADO_PENDIENTE_APLICACION);
						} else if (consultaForm.getTransaccion()
								.getTipoTransaccion().getDescripcion().trim()
								.equals(
										TipoTransaccion.PAGO_ISSS
												.getDescripcion())
								|| consultaForm
										.getTransaccion()
										.getTipoTransaccion()
										.getDescripcion()
										.trim()
										.equals(
												TipoTransaccion.PAGO_PLANILLA_ISSS_EDUCO
														.getDescripcion())) {

							int pasoTransaccion = 3;

							banderaRechazoPago = ServiciosTransaccionesYConsultas
									.fechaCortePago_ISSS_consultas(55, mapping,
											parametros, 1, pasoTransaccion);
							if (!parametros.getErrors().isEmpty()) {
								transaccion
										.setEstado(EstadoTransaccion.ESTADO_RECHAZADA);
								HibernateMap.update(transaccion);
								TransaccionUtils
										.pasarTransaccionAHistorica_v2(transaccion);
								parametros
										.getMessages()
										.add(
												"autorizacion",
												new ActionMessage(
														"mensaje.simple",
														"La Transacci&oacute;n ha sido rechazada"));
							} else
								transaccion
										.setEstado(EstadoTransaccion.ESTADO_PENDIENTE_APLICACION);
						} else { // cpalacios; Dic2013/Ene2014; fechas de
									// corte ISSS/AFP; SINO cumple condicion se
									// hace todo NORMAL como ya se hacia.
							transaccion
									.setEstado(EstadoTransaccion.ESTADO_PENDIENTE_APLICACION);
							System.out
									.println("ConsultaTransaccionAction.executeAutorizar: Cambiando estado de transaccion "
											+ transaccion.getId()
											+ " a Pendiente de Aplicacion");
							// banderaRechazoPago = true;
						}
					}
					// cpalacios; Dic2013; fechas de corte ISSS/AFP; se agrega
					// if para que solo ingrese el update cuando no realice
					// cambio por rechazo de la transaccion
					if (banderaRechazoPago == true) {
						HibernateMap.update(transaccion);
						// cpalacios; 11feb2014
						parametros
								.getMessages()
								.add(
										"autorizacion",
										new ActionMessage(
												"mensajes.detalletransaccion.autorizada.completa"));
					}
				} else {
					parametros.getMessages().add(
							"autorizacion",
							new ActionMessage(
									"mensajes.detalletransaccion.autorizada"));
				}
			} else {

				if (consultaForm.getTransaccion().getEstado().equals(
						EstadoTransaccion.ESTADO_APLICADA)
						|| consultaForm.getTransaccion().getEstado().equals(
								EstadoTransaccion.ESTADO_RECHAZADA)
						|| consultaForm.getTransaccion().getEstado().equals(
								EstadoTransaccion.ESTADO_APLICANDO)) {
					// La transaccion ya esta aplicada o esta siendo aplicada
					parametros.getErrors().add(
							"autorizacion",
							new ActionError(
									"errors.detalletransaccion.yaprocesada"));
				} else {
					if (consultaForm.getTransaccion().getEstado().equals(
							EstadoTransaccion.ESTADO_PENDIENTE_APLICACION)) {
						// La transaccion ya esta autorizada, solo esta
						// pendiente de aplicar
						parametros
								.getErrors()
								.add(
										"autorizacion",
										new ActionError(
												"errors.detalletransaccion.yaautorizada"));
					} else {
						// cualquier otro estado diferente a pendiente de
						// autorizar
						parametros
								.getErrors()
								.add(
										"autorizacion",
										new ActionError(
												"errors.detalletransaccion.noautorizada"));
						parametros.getErrors().add(
								"autorizacion",
								new ActionError(
										"errors.detalletransaccion.noestado"));
					}
				}
			}
		}
		parametros.setForward(executeConsultaDetallada(session, consultaForm,
				mapping, parametros));
	}

	/**
	 * 
	 * @param mapping
	 * @param consultaForm
	 * @param request
	 * @return
	 * @throws Exception
	 */
	private ActionForward executePaginar(ActionMapping mapping,
			ConsultaTransaccionForm consultaForm, HttpServletRequest request)
			throws Exception {
		HttpSession session = request.getSession(false);
		Usuario usuario = getUsuario(session);

		// Prepara los errores, mensajes, session, y forward
		ActionErrors errors = new ActionErrors();
		ActionForward forward = new ActionForward();

		// Pasamos los registros de la session al form de respuesta, manteniendo
		// el numero de pagina a la que queremos ir
		int pagina = consultaForm.getCurrentPage();
		consultaForm.setCurrentPage(pagina);

		forward = mapping.findForward("success");
		return forward;
	}

	// funcion que se ejecuta al eliminar una transaccion.. ahorita es solo para
	// afp
	private void executeEliminar(Transaccion trans) throws Exception {
		if (trans.getTipoTransaccion().equals(
				trans.getTipoTransaccion().PAGO_AFP)) {
			PagoAFPDB pagoAfp = new PagoAFPDB();
			java.util.List lst = trans.getLineaTransaccionList();
			java.util.Iterator it = lst.iterator();
			while (it.hasNext()) {
				LineaTransaccion lin = (LineaTransaccion) it.next();
				if (lin.esCredito()) {
					pagoAfp.setFromBytes(lin.getMemoAsBytes());
					java.io.File f = pagoAfp.getZipFilePath();
					f.delete();
				}
			}

		}
	}

	/**
	 * Ayuda a validar si los servicios de validacion de fondos estan
	 * disponibles antes de enviar a aplicar una transaccion multiple, evitando
	 * malestar de los clientes.
	 * 
	 * @param trx
	 * @return
	 */
	private boolean validaDisponibilidadServicios(Transaccion trx) {
		boolean retval = true;
		try {
			String ctaCargo = null;
			long idCliente = trx.getCliente().getId();
			// Valida que sea una transaccion multiple
			if (trx.getTipoTransaccion().getTipoOperacion().equals(
					TipoOperacion.MULTIPLE)) {
				// Valida que NO sea una "Cargos a Terceros"
				if (!trx.getTipoTransaccion().equals(
						TipoTransaccion.CARGOS_A_TERCEROS)
						&& !trx.getTipoTransaccion().equals(
								TipoTransaccion.CARGOS_AUTOMATICOS)
						&& !trx.getTipoTransaccion().equals(
								TipoTransaccion.CONFIRMACION_CHEQUES)) {
					// Busca la primera cuenta de cargo
					Iterator lista = trx.getLineaTransaccionList().iterator();
					LineaTransaccion linea = null;
					while (lista != null && lista.hasNext()) {
						linea = ((LineaTransaccion) lista.next());
						if (linea.esDebito()) {
							ctaCargo = linea.getCuentaAsString();
							linea = null;
							break;
						}
					}
					lista = null;
					retval = Servicios.tieneServiciosDisponibles(idCliente,
							ctaCargo);
				}
			}
			// busca una cuenta de cargo utilizada.
		} catch (Exception ex) {
			System.err
					.println("ConsultaTransaccionAction.validaDisponibilidadServicios() Falla encontrada: "
							+ ex.getMessage());
			ex.printStackTrace();
		}
		return retval;
	}

	public boolean esTransaccionACH(TipoTransaccion tipo) {
		return (tipo.equals(TipoTransaccion.TRANSF_PROPIAS_ACH)
				|| tipo.equals(TipoTransaccion.TRANSF_TERCEROS_ACH)
				|| tipo.equals(TipoTransaccion.PAGO_PRESTAMO_ACH)
				|| tipo.equals(TipoTransaccion.PAGO_TARJETA_ACH)
				|| tipo.equals(TipoTransaccion.PAGO_PLANILLA_ACH)
				|| tipo.equals(TipoTransaccion.PAGO_PROVEEDORES_ACH)
				|| tipo.equals(TipoTransaccion.PAGADURIAS_ACH) || tipo
				.equals(TipoTransaccion.PAGO_DE_PENSIONES_ACH));
	}

	/**
	 * Funcion verificadora si un usuario es firmante
	 * 
	 * @param usuario
	 * @return
	 */
	private boolean esUsuarioFirmante(Usuario usuario) {
		boolean esFirmante = false;
		try {
			if ((!usuario.getUsuario().getEstadoUsuario().equals(
					EstadoUsuario.ELIMINADO))
					&& (!UsuarioFlag.CREADO_LOCALMENTE.estaEn(usuario
							.getUsuario().getFlags())))
				esFirmante = true;
		} catch (Exception e) {
			esFirmante = false;
		}
		return esFirmante;

		/*
		 * 
		 * if
		 * (usuarios[u].getUsuario().getEstadoUsuario().equals(EstadoUsuario.ELIMINADO)) {
		 * firmante = false; } else if
		 * (UsuarioFlag.CREADO_LOCALMENTE.estaEn(usuarios[u].getUsuario().getFlags())) {
		 * firmante = false; } else { firmante = true; }
		 */
	}

	/**
	 * @param session
	 * @param consultaForm
	 * @param mapping
	 * @param parametros
	 * @throws Exception
	 */
	private void executeEliminarLote(HttpSession session,
			ConsultaTransaccionForm consultaForm, ActionMapping mapping,
			ParametrosConsulta parametros) throws Exception {
		StringBuffer str = new StringBuffer("");
		Number secuencial;
		int eliminadas = 0;
		Transaccion transaccion;
		ActionMessages mensajes = new ActionMessages();

		Usuario usuario = getUsuario(session);
		Cliente cliente = usuario.getCliente();

		parametros.setMessages(mensajes);
		for (int i = 0; i < consultaForm.getListaAutorizar().length; i++) {
			secuencial = new Long(consultaForm.getListaAutorizar()[i]);
			transaccion = TransaccionUtils.loadTransaccion(cliente, secuencial
					.longValue());
			if (transaccion.getEstado().equals(
					EstadoTransaccion.ESTADO_PENDIENTE_APLICACION)
					|| transaccion.getEstado().equals(
							EstadoTransaccion.ESTADO_PENDIENTE_AUTORIZACION)
					|| transaccion.getEstado().equals(
							EstadoTransaccion.ESTADO_PROGRAMADA_POR_APLICAR)) {
				TransaccionUtils.eliminar(transaccion, usuario);
				eliminadas++;
			} else {
				// cualquier otro estado diferente a pendiente de autorizar
				parametros.getErrors().add(
						"eliminar",
						new ActionError(
								"mensajes.detalletransaccion.noaplicando"));
			}
		}
		if (eliminadas > 0) {
			str.append(eliminadas);
			parametros.getMessages().add(
					"eliminar",
					new ActionMessage("mensajes.detalletransaccion.eliminada",
							str.toString()));
		} else {
			str.append("Ninguna transaccion fue aplicada!");
			parametros.getMessages().add(
					"eliminar",
					new ActionMessage("mensajes.detalletransaccion.eliminada",
							str.toString()));
		}
	}

	/**
	 * Clase para no estar declarando los errors y los forward en cada action
	 * 
	 * @author psaenz
	 */
	public class ParametrosConsulta {
		ActionErrors mErrors = new ActionErrors();

		ActionForward mForward = new ActionForward();

		ActionMessages mMessages = new ActionMessages();

		public ParametrosConsulta(ActionErrors errors, ActionForward forward,
				ActionMessages messages) {
			mErrors = errors;
			mForward = forward;
			mMessages = messages;
		}

		/**
		 * @return
		 */
		public ActionErrors getErrors() {
			return mErrors;
		}

		/**
		 * @return
		 */
		public ActionForward getForward() {
			return mForward;
		}

		/**
		 * @return
		 */
		public ActionMessages getMessages() {
			return mMessages;
		}

		/**
		 * @param errors
		 */
		public void setErrors(ActionErrors errors) {
			this.mErrors = errors;
		}

		/**
		 * @param forward
		 */
		public void setForward(ActionForward forward) {
			this.mForward = forward;
		}

		/**
		 * @param messages
		 */
		public void setMessages(ActionMessages messages) {
			this.mMessages = messages;
		}

	}

	/**
	 * Permite ordenar los registros dependiedo de los parametros dados
	 * 
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	public ActionForward executeOrdenar(ActionMapping mapping,
			ConsultaTransaccionForm consultaForm, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		ActionErrors errors = new ActionErrors();
		ActionForward forward = new ActionForward();

		List listaCheques;

		try {
			// Toma los registros de la session
			listaCheques = consultaForm.getListaTransacciones().getRegistros();

			// Toma el parametro por el cual se van a ordenar los registros
			String campo = consultaForm.getOrderBy();

			// Valida que el campo para ordenar sea valido y que la lista tenga
			// registros
			if (null == campo || "".equals(campo) || listaCheques.size() <= 0) {
				return (mapping.findForward("failure"));
			}

			String direccion = consultaForm.getDireccion();

			final int orden = ("asc".equals(direccion) ? -1 : 1);

			if ("id".equalsIgnoreCase(campo)) {
				Collections.sort(listaCheques, new Comparator() {
					public int compare(Object obj1, Object obj2) {
						if (obj1 instanceof Transaccion
								&& obj2 instanceof Transaccion) {
							Transaccion lin1 = (Transaccion) obj1;
							Transaccion lin2 = (Transaccion) obj2;
							return orden
									* new Long(lin1.getSecuencial())
											.compareTo(new Long(lin2
													.getSecuencial()));
						} else if (obj1 instanceof TransaccionHistorica
								&& obj2 instanceof TransaccionHistorica) {
							TransaccionHistorica lin1 = (TransaccionHistorica) obj1;
							TransaccionHistorica lin2 = (TransaccionHistorica) obj2;
							return orden
									* new Long(lin1.getSecuencial())
											.compareTo(new Long(lin2
													.getSecuencial()));
						} else if (obj1 instanceof Transaccion
								&& obj2 instanceof TransaccionHistorica) {
							Transaccion lin1 = (Transaccion) obj1;
							TransaccionHistorica lin2 = (TransaccionHistorica) obj2;
							return orden
									* new Long(lin1.getSecuencial())
											.compareTo(new Long(lin2
													.getSecuencial()));
						} else if (obj1 instanceof TransaccionHistorica
								&& obj2 instanceof Transaccion) {
							TransaccionHistorica lin1 = (TransaccionHistorica) obj1;
							Transaccion lin2 = (Transaccion) obj2;
							return orden
									* new Long(lin1.getSecuencial())
											.compareTo(new Long(lin2
													.getSecuencial()));
						}
						return 0;
					}
				});
			}

			if ("tipoTrx".equalsIgnoreCase(campo)) {
				Collections.sort(listaCheques, new Comparator() {
					public int compare(Object obj1, Object obj2) {
						if (obj1 instanceof Transaccion
								&& obj2 instanceof Transaccion) {
							Transaccion lin1 = (Transaccion) obj1;
							Transaccion lin2 = (Transaccion) obj2;
							return orden
									* lin1.getTipoTransaccion().compareTo(
											lin2.getTipoTransaccion());
						} else if (obj1 instanceof TransaccionHistorica
								&& obj2 instanceof TransaccionHistorica) {
							TransaccionHistorica lin1 = (TransaccionHistorica) obj1;
							TransaccionHistorica lin2 = (TransaccionHistorica) obj2;
							return orden
									* lin1.getTipoTransaccion().compareTo(
											lin2.getTipoTransaccion());
						} else if (obj1 instanceof Transaccion
								&& obj2 instanceof TransaccionHistorica) {
							Transaccion lin1 = (Transaccion) obj1;
							TransaccionHistorica lin2 = (TransaccionHistorica) obj2;
							return orden
									* lin1.getTipoTransaccion().compareTo(
											lin2.getTipoTransaccion());
						} else if (obj1 instanceof TransaccionHistorica
								&& obj2 instanceof Transaccion) {
							TransaccionHistorica lin1 = (TransaccionHistorica) obj1;
							Transaccion lin2 = (Transaccion) obj2;
							return orden
									* lin1.getTipoTransaccion().compareTo(
											lin2.getTipoTransaccion());
						}
						return 0;
					}
				});
			}

			if ("fecha".equalsIgnoreCase(campo)) {
				Collections.sort(listaCheques, new Comparator() {
					public int compare(Object obj1, Object obj2) {
						if (obj1 instanceof Transaccion
								&& obj2 instanceof Transaccion) {
							Transaccion lin1 = (Transaccion) obj1;
							Transaccion lin2 = (Transaccion) obj2;
							return orden
									* lin1.getFechaAplicacion().compareTo(
											lin2.getFechaAplicacion());
						} else if (obj1 instanceof TransaccionHistorica
								&& obj2 instanceof TransaccionHistorica) {
							TransaccionHistorica lin1 = (TransaccionHistorica) obj1;
							TransaccionHistorica lin2 = (TransaccionHistorica) obj2;
							return orden
									* lin1.getFechaAplicacion().compareTo(
											lin2.getFechaAplicacion());
						} else if (obj1 instanceof Transaccion
								&& obj2 instanceof TransaccionHistorica) {
							Transaccion lin1 = (Transaccion) obj1;
							TransaccionHistorica lin2 = (TransaccionHistorica) obj2;
							return orden
									* lin1.getFechaAplicacion().compareTo(
											lin2.getFechaAplicacion());
						} else if (obj1 instanceof TransaccionHistorica
								&& obj2 instanceof Transaccion) {
							TransaccionHistorica lin1 = (TransaccionHistorica) obj1;
							Transaccion lin2 = (Transaccion) obj2;
							return orden
									* lin1.getFechaAplicacion().compareTo(
											lin2.getFechaAplicacion());
						}
						return 0;
					}
				});
			}

			if ("referencia".equalsIgnoreCase(campo)) {
				Collections.sort(listaCheques, new Comparator() {
					public int compare(Object obj1, Object obj2) {
						if (obj1 instanceof Transaccion
								&& obj2 instanceof Transaccion) {
							Transaccion lin1 = (Transaccion) obj1;
							Transaccion lin2 = (Transaccion) obj2;
							return orden
									* lin1.getReferencia().compareTo(
											lin2.getReferencia());
						} else if (obj1 instanceof TransaccionHistorica
								&& obj2 instanceof TransaccionHistorica) {
							TransaccionHistorica lin1 = (TransaccionHistorica) obj1;
							TransaccionHistorica lin2 = (TransaccionHistorica) obj2;
							return orden
									* lin1.getReferencia().compareTo(
											lin2.getReferencia());
						} else if (obj1 instanceof Transaccion
								&& obj2 instanceof TransaccionHistorica) {
							Transaccion lin1 = (Transaccion) obj1;
							TransaccionHistorica lin2 = (TransaccionHistorica) obj2;
							return orden
									* lin1.getReferencia().compareTo(
											lin2.getReferencia());
						} else if (obj1 instanceof TransaccionHistorica
								&& obj2 instanceof Transaccion) {
							TransaccionHistorica lin1 = (TransaccionHistorica) obj1;
							Transaccion lin2 = (Transaccion) obj2;
							return orden
									* lin1.getReferencia().compareTo(
											lin2.getReferencia());
						}
						return 0;
					}
				});
			}

			if ("descripcion".equalsIgnoreCase(campo)) {
				Collections.sort(listaCheques, new Comparator() {
					public int compare(Object obj1, Object obj2) {
						if (obj1 instanceof Transaccion
								&& obj2 instanceof Transaccion) {
							Transaccion lin1 = (Transaccion) obj1;
							Transaccion lin2 = (Transaccion) obj2;
							return orden
									* lin1.getDescripcion().compareTo(
											lin2.getDescripcion());
						} else if (obj1 instanceof TransaccionHistorica
								&& obj2 instanceof TransaccionHistorica) {
							TransaccionHistorica lin1 = (TransaccionHistorica) obj1;
							TransaccionHistorica lin2 = (TransaccionHistorica) obj2;
							return orden
									* lin1.getDescripcion().compareTo(
											lin2.getDescripcion());
						} else if (obj1 instanceof Transaccion
								&& obj2 instanceof TransaccionHistorica) {
							Transaccion lin1 = (Transaccion) obj1;
							TransaccionHistorica lin2 = (TransaccionHistorica) obj2;
							return orden
									* lin1.getDescripcion().compareTo(
											lin2.getDescripcion());
						} else if (obj1 instanceof TransaccionHistorica
								&& obj2 instanceof Transaccion) {
							TransaccionHistorica lin1 = (TransaccionHistorica) obj1;
							Transaccion lin2 = (Transaccion) obj2;
							return orden
									* lin1.getDescripcion().compareTo(
											lin2.getDescripcion());
						}
						return 0;
					}
				});
			}

			if ("usuario".equalsIgnoreCase(campo)) {
				Collections.sort(listaCheques, new Comparator() {
					public int compare(Object obj1, Object obj2) {
						if (obj1 instanceof Transaccion
								&& obj2 instanceof Transaccion) {
							Transaccion lin1 = (Transaccion) obj1;
							Transaccion lin2 = (Transaccion) obj2;
							return orden
									* lin1
											.getUsuario()
											.getUsuarioAsString()
											.compareTo(
													lin2
															.getUsuario()
															.getUsuarioAsString());
						} else if (obj1 instanceof TransaccionHistorica
								&& obj2 instanceof TransaccionHistorica) {
							TransaccionHistorica lin1 = (TransaccionHistorica) obj1;
							TransaccionHistorica lin2 = (TransaccionHistorica) obj2;
							return orden
									* lin1
											.getUsuario()
											.getUsuarioAsString()
											.compareTo(
													lin2
															.getUsuario()
															.getUsuarioAsString());
						} else if (obj1 instanceof Transaccion
								&& obj2 instanceof TransaccionHistorica) {
							Transaccion lin1 = (Transaccion) obj1;
							TransaccionHistorica lin2 = (TransaccionHistorica) obj2;
							return orden
									* lin1
											.getUsuario()
											.getUsuarioAsString()
											.compareTo(
													lin2
															.getUsuario()
															.getUsuarioAsString());
						} else if (obj1 instanceof TransaccionHistorica
								&& obj2 instanceof Transaccion) {
							TransaccionHistorica lin1 = (TransaccionHistorica) obj1;
							Transaccion lin2 = (Transaccion) obj2;
							return orden
									* lin1
											.getUsuario()
											.getUsuarioAsString()
											.compareTo(
													lin2
															.getUsuario()
															.getUsuarioAsString());
						}
						return 0;
					}
				});
			}

			if ("estado".equalsIgnoreCase(campo)) {
				Collections.sort(listaCheques, new Comparator() {
					public int compare(Object obj1, Object obj2) {
						if (obj1 instanceof Transaccion
								&& obj2 instanceof Transaccion) {
							Transaccion lin1 = (Transaccion) obj1;
							Transaccion lin2 = (Transaccion) obj2;
							return orden
									* lin1.getEstado().getDescripcion()
											.compareTo(
													lin2.getEstado()
															.getDescripcion());
						} else if (obj1 instanceof TransaccionHistorica
								&& obj2 instanceof TransaccionHistorica) {
							TransaccionHistorica lin1 = (TransaccionHistorica) obj1;
							TransaccionHistorica lin2 = (TransaccionHistorica) obj2;
							return orden
									* lin1.getEstado().getDescripcion()
											.compareTo(
													lin2.getEstado()
															.getDescripcion());
						} else if (obj1 instanceof Transaccion
								&& obj2 instanceof TransaccionHistorica) {
							Transaccion lin1 = (Transaccion) obj1;
							TransaccionHistorica lin2 = (TransaccionHistorica) obj2;
							return orden
									* lin1.getEstado().getDescripcion()
											.compareTo(
													lin2.getEstado()
															.getDescripcion());
						} else if (obj1 instanceof TransaccionHistorica
								&& obj2 instanceof Transaccion) {
							TransaccionHistorica lin1 = (TransaccionHistorica) obj1;
							Transaccion lin2 = (Transaccion) obj2;
							return orden
									* lin1.getEstado().getDescripcion()
											.compareTo(
													lin2.getEstado()
															.getDescripcion());
						}
						return 0;
					}
				});
			}

			// Devolvemos los resultados en la session
			consultaForm.getListaLineasTransaccion().setRegistros(listaCheques);

			// Refresca el numero de pagina
			// consultaForm.goToPage(consultaForm.getCurrentPage());

			// Setea la direccion para que cambio de ascendente a desendente.
			consultaForm.setDireccion((orden == -1 ? "des" : "asc"));
		} catch (Exception e) {
			mostrarErrorInesperado(e, mapping, errors);
		}

		if (!errors.isEmpty()) {
			saveErrors(request, errors);
		} else {
			forward = mapping.findForward("vertransacciones");
		}

		// Finish with
		return forward;
	}

	public void limpiarParametros(ConsultaTransaccionForm consultaForm) {
		consultaForm.setTiposTransaccion(null);
		consultaForm.setListaIdTransaccion(null);
		consultaForm.setCuentasCargadas(null);
		consultaForm.setUsuariosCreacion(null);
		consultaForm.setClientesTransaccion(null);
		consultaForm.setPorMonto(false);
		consultaForm.setUsarMonto("1");
		consultaForm.setMontoDesde(0.0);
		consultaForm.setMontoHasta(0.0);
	}

	static class ValueComparator implements Comparator {

		Map map;

		public ValueComparator(Map map) {
			this.map = map;
		}

		public int compare(Object keyA, Object keyB) {
			Comparable valueA = (Comparable) map.get(keyA);
			Comparable valueB = (Comparable) map.get(keyB);
			return valueB.compareTo(valueA) * -1;
		}
	}

	public static SortedMap sortByValue(Map unsortedMap) {
		SortedMap sortedMap = new TreeMap(new ValueComparator(unsortedMap));
		sortedMap.putAll(unsortedMap);
		return sortedMap;
	}

	// cpalacios; octubre2015
	public static void insertarLogTransRechazadas(ConsultaTransaccionForm form,
			Usuario usuario) {
		long idTransaccion = form.getIdTransaccion();
		long cliente = form.getUsuario().getCliente().getId();
		String usuarioVar = usuario.getUser();

		usuarioVar = usuarioVar.substring(usuarioVar.indexOf("]") + 1);

		TransaccionesRechazadasSQL.insertarLogTransRechazadasVistas(usuarioVar,
				cliente, idTransaccion);
	}

	private boolean cambioParametrosMayoresHistorico(
			ConsultaTransaccionForm form) {
		/***********************************************************************
		 * Verificamos si ha existido un cambio de parametros principales, esto
		 * para controlar la búsqueda si se hace por el método de parametros
		 * principales, o sino combinado con los otros parametros
		 **********************************************************************/
		if ((form.getEstadoAuditoria().equals(form.getOldEstado())
				&& form.getFechaDesde().equals(form.getOldFechaDesde()) && form
				.getFechaHasta().equals(form.getOldFechaHasta()))) {
			return false;
		} else {
			return true;
		}
	}

	private void resetFormHistorico(ConsultaTransaccionForm form,
			ActionMapping mapping, HttpServletRequest request) {
		form.setFechaDesdeDia(null);
		form.setFechaDesdeMes(null);
		form.setFechaDesdeAnno(null);
		form.setFechaHastaDia(null);
		form.setFechaHastaMes(null);
		form.setFechaHastaAnno(null);
		form.reset(mapping, request);
		form.setEstadoAuditoria("todos");
		form.setFiltroTipoTransaccion("none");
		form.setUsarMonto("1");
		request.removeAttribute("lstUsrTmp");
	}
}
