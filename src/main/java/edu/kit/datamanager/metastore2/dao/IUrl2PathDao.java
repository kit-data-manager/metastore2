/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.dao;

import edu.kit.datamanager.metastore2.domain.Url2Path;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/**
 * Database linking URL to local path (if available)
 * @author Torridity
 */
public interface IUrl2PathDao extends JpaRepository<Url2Path, String>, JpaSpecificationExecutor<Url2Path>{
  Optional<Url2Path>  findByUrl(String url);
  List<Url2Path>      findByPath(String path);
}
