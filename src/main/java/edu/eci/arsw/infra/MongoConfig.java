package edu.eci.arsw.infra;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
/** Configuración de MongoDB con auditoría habilitada */
@Configuration
@EnableMongoAuditing
public class MongoConfig { }
