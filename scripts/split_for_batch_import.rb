#!/usr/bin/env ruby

require 'csv'
require 'securerandom'

# We need to read in a CSV like we're getting from Tom Helmuth, and then
# generate two new CSV files, one with the nodes and one with the edges
# (i.e., parent-child relationships).

# We may need to do something with indexing, but auto-indexing may take
# care of that for us.

# The plan is that you'll call this like:
#   ruby split_for_batch_imports data6.csv
# and it will generate two new files: data6_nodes.csv and data6_edges.csv

# The headers for replace-space-with-newline are:
#   uuid,generation,location,parent_uuids,genetic_operators,
#   push_program_size,plush_genome_size,push_program,plush_genome,
#   total_error,TC0,TC1,TC2,...,TC199
# We need to replace all the dashes in the headers with underscores,
# and it would be good to glob all the errors into an array instead of
# individual columns.

input_file = ARGV[0]
dirname = File.dirname(input_file)
basename = File.basename(input_file, ".csv")
node_file = File.join(dirname, basename + "_nodes.csv")
edge_file = File.join(dirname, basename + "_edges.csv")

run_uuid = SecureRandom.uuid()

dashes_to_newlines = lambda { |str| str.gsub('-', '_') }

def parse_parent_uuids(str)
  str = str[1...-1]
  parent_uuids = str.split(" ")
end

printed_headers = false
CSV.open(node_file, "wb") do |nodes|
  CSV.open(edge_file, "wb") do |edges|
    num_rows = 0
    edges << ["uuid:string:individuals", "uuid:string:individuals", "type"]
    CSV.open(input_file, "r",
    :headers => true,
    :header_converters => dashes_to_newlines,
    :converters => [:numeric]) do |inputs|
      inputs.each do |row|
        if not printed_headers
          headers = inputs.headers
          headers -= ["parent_uuids"]
          headers += ["run_uuid"]
          headers[headers.index("uuid")] = "uuid:string:individuals"
          nodes << headers
          printed_headers = true
        end
        parent_ids = parse_parent_uuids(row["parent_uuids"])
        row.delete("parent_uuids")
        row["run_uuid"] = run_uuid
        row["uuid"] = '"' + row["uuid"] + '"'
        row["plush_genome"] = row["plush_genome"].gsub("\\", "\\\\\\")
        row["push_program"] = row["push_program"].gsub("\\", "\\\\\\")
        nodes << row
        parent_ids.each do |parent_uuid|
          edges << [parent_uuid, row["uuid"], "PARENT_OF"]
        end
      end
    end
  end
end

# Syntax for calling batch_import:
#    ./import.sh test.db ../data/data6_nodes.csv ../data/data6_edges.csv