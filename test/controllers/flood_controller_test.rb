require 'test_helper'

class FloodControllerTest < ActionDispatch::IntegrationTest
  test "should get index" do
    get flood_index_url
    assert_response :success
  end

  test "should get map" do
    get flood_map_url
    assert_response :success
  end

end
