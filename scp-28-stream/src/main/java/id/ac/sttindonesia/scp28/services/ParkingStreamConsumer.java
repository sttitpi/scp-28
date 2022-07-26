package id.ac.sttindonesia.scp28.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.sttindonesia.scp28.CarParkingConfig;
import id.ac.sttindonesia.scp28.KafkaConfig;
import id.ac.sttindonesia.scp28.model.SensorModel;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class ParkingStreamConsumer 
{  
  Logger logger = LoggerFactory.getLogger(ParkingStreamConsumer.class);
 
  @Autowired
  KafkaConfig kafkaConfig;
  
  @Autowired
  CarParkingConfig carparkConfig;
  
  public void consume()
  {    
    KafkaConsumer<String, String> consumer = new KafkaConsumer<>(kafkaConfig.getPropConsumers());
    consumer.subscribe(Arrays.asList("sensor-1", "sensor-2", "sensor-3", "sensor-4"));
    int empty_time = 0;
    int duration = 100;
    while (true) {
      ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(duration));
      if (records.count() > 0)
      {
        empty_time = 0;
        for (ConsumerRecord<String, String> record : records)
        {
          System.out.printf("offset = %d, key = %s, value = %s%n", record.offset(), record.key(), record.value());
//          postSensor(record.value());
        }
      }
      else        
      {
        empty_time += duration;
      }
      
      if (empty_time > 60000)
      {
        //ini problem bagaimana mencari sensor_id yang kosong
        this.postSensor("{\"sensorId\":\"1\",\"data\":{\"status\":\"0\"}}");
        this.postSensor("{\"sensorId\":\"2\",\"data\":{\"status\":\"0\"}}");
        this.postSensor("{\"sensorId\":\"3\",\"data\":{\"status\":\"0\"}}");
        this.postSensor("{\"sensorId\":\"4\",\"data\":{\"status\":\"0\"}}");

        empty_time = 0;
      }      
    }
  }
  public void postSensor(String record)
  {    
    MultiValueMap<String, String> headers = new LinkedMultiValueMap();
    Map map = new HashMap<String, String>();
    map.put("Content-Type", "application/json");
    map.put("Accept", "application/json");

    headers.setAll(map);
    
    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
    try
    {
      SensorModel sensor = mapper.readValue(record, SensorModel.class);
      logger.info("sensorId: " + sensor.getSensorId());
      logger.info("status: " + sensor.getData().getStatus());

      Map req_payload = new HashMap();
      req_payload.put("sensor_id", sensor.getSensorId());
      req_payload.put("label", "slot-" + sensor.getSensorId());
      req_payload.put("status", sensor.getData().getStatus());

      HttpEntity<?> request = new HttpEntity<>(req_payload, headers);
      String url = carparkConfig.getApiURLv1() + "/sensor";

      ResponseEntity<?> response = new RestTemplate().postForEntity(url, request, String.class);  
      logger.info("" + response.getStatusCode());
    }
    catch (JsonProcessingException e)
    {
      logger.error(e.getMessage());
    }
    
    
  }
}

