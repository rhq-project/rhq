#
# RHQ Management Platform
# Copyright (C) 2005-2013 Red Hat, Inc.
# All rights reserved.
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation version 2 of the License.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
#

# Notes:
#1) All times are expressed in milliseconds because the database stores timestamps in milliseconds
#2) The data is randomly generated and does represent a live system collection patterns
#3) The amount of data generated is based on estimates described at:
#   https://docs.jboss.org/author/display/RHQ/Metrics+Data+Migration+-+Design
#4) The data generated is in a state consistent with the migration process
#5) !!!! All existing data is wiped out of the existing database tables and new data only is written back!!!
#   !!!! You will lose your data if your this script without manually disabling the data deletion part !!!!
#6) Expect the generation process to take about 60 seconds per agent
#7) The script is configured to generate data for 10 agents, if you can increase that limit by updating the variables below

import psycopg2
import sys
import random
import time
import cStringIO

class MetricType:
   Raw,Aggregate = range(2)


RANDOM_DATA_FACTOR = 10000;


def generate_random_metrics(number_of_metrics = 1000, start_timestamp = 0, number_of_schedule_ids = 1, start_of_schedule_id_sequence = 1, metricType = MetricType.Raw):
   #generate a random series of timestamps for the random metrics
   end_timestamp = start_timestamp + number_of_metrics * 10;
   timestamps = random.sample(range(start_timestamp,end_timestamp),number_of_metrics)

   #generate a random set of schedule ids
   end_of_schedule_id_sequence = start_of_schedule_id_sequence + number_of_schedule_ids * 100;
   schedule_ids = random.sample(range(start_of_schedule_id_sequence,end_of_schedule_id_sequence),number_of_schedule_ids);

   #the set of random metrics
   random_metrics = []

   #generate the random metrics
   for i in range(number_of_metrics):
      if  metricType == MetricType.Raw :
         value = generate_random_raw_value()
      else :
         value = generate_random_aggregate_value()

      random_metric = ( random.choice(schedule_ids) , timestamps[i]) + value
      random_metrics.append(random_metric);

   return random_metrics


def generate_random_raw_value():
   #raw metrics have just value
   return (random.random()*RANDOM_DATA_FACTOR,)


def generate_random_aggregate_value():
   #aggregate metrics have min, max, and value
   random_values = [random.random()*RANDOM_DATA_FACTOR,random.random()*RANDOM_DATA_FACTOR, random.random()*RANDOM_DATA_FACTOR]
   min_value = min(random_values)
   max_value = max(random_values)
   random_values.remove(min_value)
   random_values.remove(max_value)
   average_value = random_values.pop()

   return (average_value, min_value, max_value)


def insert_data(connection,data,table_name,table_columns):
   #do the postgres insertion using copy_from functionality since it's the fastest method
   #to bulk import data
   input_data = cStringIO.StringIO(data)
   cursor = connection.cursor()
   cursor.copy_from(input_data,sep="\t",table = table_name, columns = table_columns)
   cursor.close()
   connection.commit()


def delete_table_data(connection, table):
   delete_statement = "DELETE FROM " + table;
   connection.cursor().execute(delete_statement)
   connection.commit()



#Main Script

script_start_time = time.time()

#General Configuration
raw_tables = map(lambda x: "RHQ_MEAS_DATA_NUM_R"+str(x).zfill(2),range(15))
raw_table_columns = ["schedule_id","time_stamp","value"]

aggregate_tables = map(lambda x: "RHQ_MEASUREMENT_DATA_NUM_"+str(x),["1H","6H","1D"])
aggregate_table_columns = ["schedule_id","time_stamp","value","minvalue","maxvalue"]

 # see the estimation guideline at https://docs.jboss.org/author/display/RHQ/Metrics+Data+Migration+-+Design
raw_metrics_per_agent = 900000
aggregate_metrics_per_agent = 700000
number_of_agents = 10

# some constants to be used by the algorithm
batch_increment = 10000
data_start_time = int(time.time() - 2*604800)*1000 # now - 2 week, convert to milliseconds

#establish the db connection, update the settings for your local environment
connection = psycopg2.connect(database="rhq_db",user="rhqadmin",password="rhqadmin")


#Delete all data available
map(lambda x : delete_table_data(connection, x), raw_tables)
map(lambda x : delete_table_data(connection, x), aggregate_tables)


#Generate random raw and aggregate data
for j in range(number_of_agents) :
   agent_generation_start_time = time.time()

   #generate and insert random metrics
   for i in range(0,raw_metrics_per_agent,batch_increment):
      current_batch = i / batch_increment
      random_raw_table = random.choice(raw_tables)

      raw_metrics =  generate_random_metrics( number_of_metrics = batch_increment, start_timestamp = data_start_time + current_batch*batch_increment*10, \
                                              start_of_schedule_id_sequence = j * batch_increment, metricType = MetricType.Raw)

      data_to_insert = "\n".join(map(str,map(lambda x: "\t".join((str(s) for s in x)),raw_metrics)))
      insert_data(connection,data_to_insert,random_raw_table,raw_table_columns)

   #generate and insert aggregate metrics
   for i in range(0,aggregate_metrics_per_agent,batch_increment):
      current_batch = i / batch_increment
      random_aggregate_table = random.choice(aggregate_tables)

      aggregate_metrics =  generate_random_metrics( number_of_metrics = batch_increment, start_timestamp = data_start_time + current_batch*batch_increment*10, \
                                                    start_of_schedule_id_sequence = j * batch_increment, metricType = MetricType.Aggregate )

      data_to_insert = "\n".join(map(str,map(lambda x: "\t".join((str(s) for s in x)),aggregate_metrics)))
      insert_data(connection,data_to_insert,random_aggregate_table,aggregate_table_columns)

   print "Data inserted for agent #",j, " - total time:", time.time() - agent_generation_start_time, " seconds"

print "Total time: ",time.time() - script_start_time, " seconds"
