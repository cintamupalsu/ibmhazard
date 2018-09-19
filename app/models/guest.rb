class Guest < ApplicationRecord
  validates :identity,  presence: true, length: { maximum: 50 }
  mount_uploader :picture, PictureUploader
end
