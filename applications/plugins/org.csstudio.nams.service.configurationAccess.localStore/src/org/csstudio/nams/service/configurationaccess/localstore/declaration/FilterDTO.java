package org.csstudio.nams.service.configurationaccess.localstore.declaration;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.csstudio.nams.service.configurationaccess.localstore.Mapper;
import org.csstudio.nams.service.configurationaccess.localstore.internalDTOs.FilterConditionDTO;
import org.csstudio.nams.service.configurationaccess.localstore.internalDTOs.filterConditionSpecifics.FilterConditionsToFilterDTO;
import org.csstudio.nams.service.configurationaccess.localstore.internalDTOs.filterConditionSpecifics.HasManuallyJoinedElements;
import org.csstudio.nams.service.configurationaccess.localstore.internalDTOs.filterConditionSpecifics.JunctorConditionForFilterTreeDTO;
import org.csstudio.nams.service.configurationaccess.localstore.internalDTOs.filterConditionSpecifics.NegationConditionForFilterTreeDTO;

/**
 * Dieses Daten-Transfer-Objekt stellt hält die Konfiguration eines Filters dar
 * 
 * Das Create-Statement für die Datenbank hat folgendes Aussehen:
 * 
 * <pre>
 * Create table AMS_Filter
 * 
 * iFilterID		INT,
 * iGroupRef		INT default -1 NOT NULL,
 * cName			VARCHAR(128),
 * cDefaultMessage	VARCHAR(1024),
 * PRIMARY KEY (iFilterID)
 * ;
 * </pre>
 */
@Entity
@Table(name = "AMS_Filter")
public class FilterDTO implements NewAMSConfigurationElementDTO,
		HasManuallyJoinedElements {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	@Column(name = "iFilterID")
	private int iFilterID; // INT,

	@Column(name = "iGroupRef", nullable = false)
	private int iGroupRef = -1; // INT default -1 NOT NULL,

	@Column(name = "cName", length = 128)
	private String name; // VARCHAR(128),

	@Column(name = "cDefaultMessage", length = 1024)
	private String defaultMessage; // VARCHAR(1024),

	@Transient
	private List<FilterConditionDTO> filterConditons = new LinkedList<FilterConditionDTO>();

	public int getIFilterID() {
		return iFilterID;
	}

	@SuppressWarnings("unused")
	private void setIFilterID(int filterID) {
		iFilterID = filterID;
	}

	/**
	 * Kategorie
	 */
	public int getIGroupRef() {
		return iGroupRef;
	}

	@SuppressWarnings("unused")
	public void setIGroupRef(int groupRef) {
		iGroupRef = groupRef;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDefaultMessage() {
		return defaultMessage;
	}

	public void setDefaultMessage(String defaultMessage) {
		this.defaultMessage = defaultMessage;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(this.getClass()
				.getSimpleName());
		builder.append(": ");
		builder.append("iFilterID: ");
		builder.append(iFilterID);
		builder.append(", iGroupRef: ");
		builder.append(iGroupRef);
		builder.append(", cName: ");
		builder.append(name);
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((defaultMessage == null) ? 0 : defaultMessage.hashCode());
		result = prime * result + iFilterID;
		result = prime * result + iGroupRef;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof FilterDTO))
			return false;
		final FilterDTO other = (FilterDTO) obj;
		if (defaultMessage == null) {
			if (other.defaultMessage != null)
				return false;
		} else if (!defaultMessage.equals(other.defaultMessage))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (iFilterID != other.iFilterID)
			return false;
		if (iGroupRef != other.iGroupRef)
			return false;
		return true;
	}

	public List<FilterConditionDTO> getFilterConditions() {
		return filterConditons;
	}

	public void setFilterConditions(List<FilterConditionDTO> filterConditonDTOs) {
		filterConditons = filterConditonDTOs;
	}

	public String getUniqueHumanReadableName() {
		return toString();
	}

	public boolean isInCategory(int categoryDBId) {
		return false;
	}

	public void deleteJoinLinkData(Mapper mapper) throws Throwable {
		List<FilterConditionsToFilterDTO> list = mapper.loadAll(
				FilterConditionsToFilterDTO.class, true);
		for (FilterConditionsToFilterDTO fctf : list) {
			if (fctf.getIFilterRef() == this.iFilterID) {
				mapper.delete(fctf);
			}
		}

		Collection<FilterConditionDTO> toRemove = new HashSet<FilterConditionDTO>();
		for (FilterConditionDTO condition : getFilterConditions()) {
			if (condition instanceof JunctorConditionForFilterTreeDTO) {
				if (condition instanceof HasManuallyJoinedElements) {
					((HasManuallyJoinedElements) condition)
							.deleteJoinLinkData(mapper);
				}
				mapper.delete(condition);
				toRemove.add(condition);
			}
			if (condition instanceof NegationConditionForFilterTreeDTO) {
				if (condition instanceof HasManuallyJoinedElements) {
					((HasManuallyJoinedElements) condition)
							.deleteJoinLinkData(mapper);
				}
				mapper.delete(condition);
				toRemove.add(condition);
			}
		}
		this.filterConditons.removeAll(toRemove);
	}
	
	private <T extends FilterConditionDTO> T findForId(int id, Collection<T> fcs) {
		for (T t : fcs) {
			if( t.getIFilterConditionID() == id ) {
				return t;
			}
		}
		return null;
	}

	public void storeJoinLinkData(Mapper mapper) throws Throwable {
		// ANdere FCs sind auf jeden Fall gespeichert.
		List<JunctorConditionForFilterTreeDTO> allJCFFT = mapper.loadAll(JunctorConditionForFilterTreeDTO.class, true);
		List<NegationConditionForFilterTreeDTO> allNots = mapper.loadAll(NegationConditionForFilterTreeDTO.class, true);
		List<FilterConditionsToFilterDTO> alleRootKnotenZuordnungen = mapper.loadAll(FilterConditionsToFilterDTO.class, true);
		
		List<FilterConditionDTO> operands = this.getFilterConditions();
		
		for (FilterConditionDTO operand : operands) {
			if (operand instanceof JunctorConditionForFilterTreeDTO) {
				JunctorConditionForFilterTreeDTO existingJCFFT = findForId(operand.getIFilterConditionID(), allJCFFT);
				
				if( existingJCFFT != null ) {
					existingJCFFT.storeJoinLinkData(mapper);
				} else {
					mapper.save(operand);
				}
				
			}
			if (operand instanceof NegationConditionForFilterTreeDTO) {
				NegationConditionForFilterTreeDTO existingNot = findForId(operand.getIFilterConditionID(), allNots);
				
				if( existingNot != null ) {
					existingNot.storeJoinLinkData(mapper);
				} else {
					mapper.save(operand);
				}
			}
		}
		
		Collection<FilterConditionsToFilterDTO> neueJoins = new HashSet<FilterConditionsToFilterDTO>();
		for (FilterConditionDTO fcFuerZuorndung : getFilterConditions()) {
			FilterConditionsToFilterDTO newJoin = new FilterConditionsToFilterDTO(this.getIFilterID(), fcFuerZuorndung.getIFilterConditionID());
			
			if( !alleRootKnotenZuordnungen.contains(newJoin) ) {
			neueJoins.add(newJoin);
			}
		}
		
		for (FilterConditionsToFilterDTO zuordnung: alleRootKnotenZuordnungen) {
			if( zuordnung.getIFilterRef() == this.getIFilterID() ) {
				FilterConditionDTO existing = findForId(zuordnung.getIFilterConditionRef(), getFilterConditions());
				if( existing == null ) {
					// Diese Zuordnung ist "tot"
					mapper.delete(zuordnung);
				} 
			}
		}
		
		for (FilterConditionsToFilterDTO toSave : neueJoins) {
			mapper.save(toSave);
		}
	}

	public void loadJoinData(Mapper mapper) throws Throwable {
		// Dieses wird außerhalb im Service gemacht!
	}

}
