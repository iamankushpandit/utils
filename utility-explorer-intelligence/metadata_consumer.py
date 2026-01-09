import json
import logging
from aiokafka import AIOKafkaConsumer
from database import SessionLocal
from db_models import MetricMetadata
from embeddings import generate_embedding
import asyncio
from sqlalchemy import text

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

KAFKA_BOOTSTRAP_SERVERS = "kafka:29092"
METADATA_TOPIC = "system.metadata.metrics"

async def consume_metadata():
    while True:
        try:
            consumer = AIOKafkaConsumer(
                METADATA_TOPIC,
                bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
                group_id="intelligence_metadata_group",
                value_deserializer=lambda m: json.loads(m.decode("utf-8")),
                auto_offset_reset='earliest'
            )
            
            await consumer.start()
            logger.info(f"Metadata Consumer connected to {METADATA_TOPIC}")
            break
        except Exception as e:
            logger.warning(f"Kafka not ready, retrying in 5s... ({e})")
            await asyncio.sleep(5)

    try:
        async for msg in consumer:
            definition = msg.value
            await process_definition(definition)
    except Exception as e:
        logger.error(f"Error consuming metadata: {e}")
    finally:
        await consumer.stop()

async def process_definition(definition):
    metric_id = definition.get("metricId")
    description = definition.get("description")
    
    if not metric_id or not description:
        logger.warning(f"Invalid metric definition received: {definition}")
        return

    logger.info(f"Processing metadata for: {metric_id}")
    
    try:
        embedding = await asyncio.to_thread(generate_embedding, description)

        db = SessionLocal()
        try:
            existing = db.query(MetricMetadata).filter(MetricMetadata.metric_id == metric_id).first()
            if existing:
                existing.description = description
                existing.unit_label = definition.get("unitLabel")
                existing.display_name = definition.get("displayName")
                existing.source_system = definition.get("sourceSystem")
                existing.embedding = embedding
            else:
                new_metric = MetricMetadata(
                    metric_id=metric_id,
                    description=description,
                    unit_label=definition.get("unitLabel"),
                    display_name=definition.get("displayName"),
                    source_system=definition.get("sourceSystem"),
                    embedding=embedding
                )
                db.add(new_metric)
            db.commit()
            logger.info(f"Successfully registered/updated metric: {metric_id}")
        except Exception as e:
            logger.error(f"DB Error processing metric {metric_id}: {e}")
            db.rollback()
        finally:
            db.close()
    except Exception as e:
         logger.error(f"Encoding Error: {e}")
