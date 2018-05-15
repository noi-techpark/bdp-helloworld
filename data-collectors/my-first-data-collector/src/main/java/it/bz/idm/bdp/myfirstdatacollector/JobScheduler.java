package it.bz.idm.bdp.myfirstdatacollector;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import it.bz.idm.bdp.dto.DataTypeDto;
import it.bz.idm.bdp.dto.StationDto;
import it.bz.idm.bdp.dto.StationList;
import it.bz.idm.bdp.myfirstdatacollector.dto.CoolDataDto;

/**
 * Cronjob configuration can be found under src/main/resources/META-INF/spring/applicationContext.xml
 */
@Component
public class JobScheduler {

	private static final Logger LOG = LogManager.getLogger(JobScheduler.class.getName());

	@Autowired
	private Environment env;

	@Autowired
	private DataPusher pusher;

	@Autowired
	private DataRetrieval retrieval;

	public void pushStations() throws Exception {
		List<CoolDataDto> data = retrieval.fetchData();

		StationList stationList = new StationList();
		for (CoolDataDto dto : data) {
			StationDto station = new StationDto();
			station.setName(dto.getStation());
			station.setStationType("Teststation");
			station.setOrigin(env.getProperty("dataset.origin")); // The source of our data set
			stationList.add(station);
		}

		pusher.syncStations(stationList);
	}

	public void pushDataTypes() throws Exception {
		List<CoolDataDto> data = retrieval.fetchData();

		List<DataTypeDto> dataTypeList = new ArrayList<DataTypeDto>();
		for (CoolDataDto dto : data) {
			DataTypeDto type = new DataTypeDto();
			type.setName(dto.getStation());
			type.setPeriod(600);
			dataTypeList.add(type);
		}

		pusher.syncDataTypes(dataTypeList);
	}

	public void pushDataToCollector() throws Exception {
		List<CoolDataDto> data = retrieval.fetchData();

		try {
			pusher.mapData(data);
			pusher.pushData();
		} catch (Exception e) {
			LOG.error("Processing of failed: {}.", e.getMessage());
			throw e;
		}
	}
}