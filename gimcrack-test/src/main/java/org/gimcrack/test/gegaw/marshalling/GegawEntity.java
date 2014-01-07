package org.gimcrack.test.gegaw.marshalling;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.SequenceGenerator;
import javax.persistence.Version;

@Entity(name="gegaw")
@SequenceGenerator(name = "gegawIdSeq", sequenceName = "GEGAW_ID_SEQ")
public class GegawEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "gegawIdSeq")
    public Long id;

    @Version
    private int version;

    private Date date;

    @Lob
    @Column(length = 2147483647)
    private byte[] rulesByteArray;

}