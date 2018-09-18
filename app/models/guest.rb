class Guest < ApplicationRecord
  validates :identity,  presence: true, length: { maximum: 50 }
end
