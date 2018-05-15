package it.bz.idm.bdp.myfirstdatacollector;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import it.bz.idm.bdp.dto.DataMapDto;
import it.bz.idm.bdp.dto.RecordDtoImpl;
import it.bz.idm.bdp.dto.SimpleRecordDto;
import it.bz.idm.bdp.json.JSONPusher;
import it.bz.idm.bdp.myfirstdatacollector.dto.CoolDataDto;

@Service
public class DataPusher extends JSONPusher {

	private static final Logger LOG = LogManager.getLogger(DataPusher.class.getName());

	@Autowired
	private Environment env;

	private DataMapDto<RecordDtoImpl> rootMap = null;

	public void pushData() {
		pushData(this.integreenTypology, rootMap);
	}

	/** Define your station type, which must be present in bdp-core/dal and derived from "Station" */
	@Override
	public String initIntegreenTypology() {
		return "Teststation";
	}

	@Override
	public <T> DataMapDto<RecordDtoImpl> mapData(T data) {

		@SuppressWarnings("unchecked")
		List<CoolDataDto> stations = ((List<CoolDataDto>) data);

		/*
		 * Create a data map and send it to the writer, which then stores it inside the
		 * database. The data map is a hierarchical structure, build as follows:
		 * rootMap --
		 *          |-- Station: "A"
		 *            |-- Parameter: "temperature"
		 *              |-- Values: { (time, 23), (time, ...) }
		 *            |-- Parameter: "pressure"
		 *              |-- Values: { (time, 1.2), (time, ...) }
		 *			|-- Station: "B"
		 *            |-- Parameter: "temperature"
		 *              |-- Values: { (time, 21), (time, ...) }
		 *            |-- Parameter: "pressure"
		 *              |-- Values: { (time, 1.5), (time, ...) }
		 *
		 * ... where values are a list of SimpleRecordDtos
		 */
		rootMap = new DataMapDto<RecordDtoImpl>();

		for (CoolDataDto station : stations) {

			try {
				DataMapDto<RecordDtoImpl> stationMap = rootMap.upsertBranch(station.getStation());
				DataMapDto<RecordDtoImpl> metricMap = stationMap.upsertBranch(station.getName());

				List<RecordDtoImpl> values = metricMap.getData();

				SimpleRecordDto simpleRecordDto = new SimpleRecordDto(station.getDate().getTime(), station.getValue());
				simpleRecordDto.setPeriod(env.getProperty("period", Integer.class));
				values.add(simpleRecordDto);
			} catch (Exception e) {
				LOG.error("Problem during data map creation: " + e.getMessage());
				e.printStackTrace();
				/*
				 * It depends on your use case, if you want to be forgiving on errors or not. Choose:
				 * - Despite all errors, continue to insert what we got...
				 * continue;
				 *
				 * ... or ...
				 * - An error occurred, we terminate the map creation.
				 */
				throw e; // we default to error = stop processing and report
			}
		}
		return rootMap;
	}
}
