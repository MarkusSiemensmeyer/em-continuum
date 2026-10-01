package io.continuum.slices.blueprint.items;

import org.springframework.data.jpa.repository.JpaRepository;

interface ItemsRepository extends JpaRepository<ItemsEntity, String> {
}
