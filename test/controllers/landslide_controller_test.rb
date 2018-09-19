require 'test_helper'

class LandslideControllerTest < ActionDispatch::IntegrationTest
  test "should get index" do
    get landslide_index_url
    assert_response :success
  end

  test "should get map" do
    get landslide_map_url
    assert_response :success
  end

end
