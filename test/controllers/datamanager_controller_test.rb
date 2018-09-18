require 'test_helper'

class DatamanagerControllerTest < ActionDispatch::IntegrationTest
  test "should get region" do
    get datamanager_region_url
    assert_response :success
  end

end
