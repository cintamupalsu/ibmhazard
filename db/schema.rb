# This file is auto-generated from the current state of the database. Instead
# of editing this file, please use the migrations feature of Active Record to
# incrementally modify your database, and then regenerate this schema definition.
#
# Note that this schema.rb definition is the authoritative source for your
# database schema. If you need to create the application database on another
# system, you should be using db:schema:load, not running all the migrations
# from scratch. The latter is a flawed and unsustainable approach (the more migrations
# you'll amass, the slower it'll run and the greater likelihood for issues).
#
# It's strongly recommended that you check this file into your version control system.

ActiveRecord::Schema.define(version: 20180918032129) do

  create_table "anchors", force: :cascade do |t|
    t.float "adj_lat"
    t.float "adj_lon"
    t.float "cellsize"
    t.string "name"
    t.float "llat"
    t.float "llon"
    t.float "nrows"
    t.float "ncols"
    t.integer "x"
    t.integer "y"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
  end

  create_table "cells", force: :cascade do |t|
    t.integer "anchor_id"
    t.integer "hazard_id"
    t.float "lat"
    t.float "lon"
    t.integer "x"
    t.integer "y"
    t.float "v"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["anchor_id"], name: "index_cells_on_anchor_id"
    t.index ["hazard_id"], name: "index_cells_on_hazard_id"
  end

  create_table "guests", force: :cascade do |t|
    t.string "identity"
    t.float "lat"
    t.float "lon"
    t.string "picture"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
  end

  create_table "hazards", force: :cascade do |t|
    t.string "name"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
  end

  create_table "nodes", force: :cascade do |t|
    t.string "osm"
    t.float "lat"
    t.float "lon"
    t.integer "zone_id"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["zone_id"], name: "index_nodes_on_zone_id"
  end

  create_table "nodetags", force: :cascade do |t|
    t.string "k"
    t.string "v"
    t.integer "node_id"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["node_id"], name: "index_nodetags_on_node_id"
  end

  create_table "rectangles", force: :cascade do |t|
    t.integer "rank"
    t.float "lat0"
    t.float "lat1"
    t.float "lat2"
    t.float "lat3"
    t.float "lon0"
    t.float "lon1"
    t.float "lon2"
    t.float "lon3"
    t.text "inside"
    t.integer "iteration"
    t.integer "hazard_id"
    t.integer "zone_id"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.integer "anchor_id"
    t.index ["anchor_id"], name: "index_rectangles_on_anchor_id"
    t.index ["hazard_id"], name: "index_rectangles_on_hazard_id"
    t.index ["zone_id"], name: "index_rectangles_on_zone_id"
  end

  create_table "regions", force: :cascade do |t|
    t.string "name"
    t.float "lat"
    t.float "lon"
    t.integer "x"
    t.integer "y"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
  end

  create_table "wayrefs", force: :cascade do |t|
    t.integer "n_node"
    t.integer "p_node"
    t.float "n_distance"
    t.float "p_distance"
    t.integer "way_id"
    t.integer "node_id"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["node_id"], name: "index_wayrefs_on_node_id"
    t.index ["way_id"], name: "index_wayrefs_on_way_id"
  end

  create_table "ways", force: :cascade do |t|
    t.string "osm"
    t.integer "zone_id"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["zone_id"], name: "index_ways_on_zone_id"
  end

  create_table "waytags", force: :cascade do |t|
    t.string "model"
    t.string "k"
    t.string "v"
    t.integer "way_id"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["way_id"], name: "index_waytags_on_way_id"
  end

  create_table "zones", force: :cascade do |t|
    t.integer "region_id"
    t.string "place"
    t.float "lat"
    t.float "lon"
    t.integer "x"
    t.integer "y"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["region_id"], name: "index_zones_on_region_id"
  end

end
