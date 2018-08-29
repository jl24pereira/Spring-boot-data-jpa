package com.pereira.springboot.app.model.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotEmpty;

@Entity
@Table(name ="facturas")
public class Factura implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;
	
	@NotEmpty
	private String descripcion;
	
	@Temporal(TemporalType.DATE)
	@Column(name="create_at")
	private Date createAt;
	
	@ManyToOne(fetch = FetchType.LAZY)
	private Cliente cliente;
	
	@OneToMany(fetch=FetchType.LAZY,cascade=CascadeType.ALL)
	@JoinColumn(name="factura_id")
	private List<DetalleFactura> detalle;
	
	public Factura() {
		detalle= new ArrayList<>();
	}

	@PrePersist
	public void prePersist() {
		createAt=new Date();
	}
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getDescripcion() {
		return descripcion;
	}
	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}
	public Date getCreateAt() {
		return createAt;
	}
	public void setCreateAt(Date create_at) {
		this.createAt = create_at;
	}
	public Cliente getCliente() {
		return cliente;
	}
	public void setCliente(Cliente cliente) {
		this.cliente = cliente;
	}

	public List<DetalleFactura> getDetalle() {
		return detalle;
	}

	public void setDetalle(List<DetalleFactura> detalle) {
		this.detalle = detalle;
	}
	
	public void addDetalle(DetalleFactura detalleFactura) {
		detalle.add(detalleFactura);
	}
	
	public Double getTotal() {
		Double total=0.0;
		for(int i=0;i<detalle.size();i++) {
			total+=detalle.get(i).calcularImporte();
		}
		return total;
	}

}
