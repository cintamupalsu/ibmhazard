require 'test_helper'

class TsunamiControllerTest < ActionDispatch::IntegrationTest
  test "should get index" do
    get tsunami_index_url
    assert_response :success
  end

  test "should get map" do
    get tsunami_map_url
    assert_response :success
  end

end
