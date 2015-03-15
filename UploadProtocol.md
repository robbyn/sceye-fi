# Upload protocol #

I will summarize here my understanding of the protocol that the Eye-Fi cards are using to upload images to the server. This information is incomplete because it was mainly obtained by reverse-engineering.

The protocol is based on SOAP/HTTP. The commands are sent by the Eye-Fi card as HTTP POSTs on the port 59278 of the server. The address of the server is given to the Eye-Fi card during configuration, by the Eye-Fi Manager. The path is either `/api/soap/eyefilm/v1` for plain XML (SOAP) messages, or `/api/soap/eyefilm/v1/upload` when a file is attached. In the latter case, a multipart content is sent.

When the Eye-Fi card wants to send an image to the server, it first sends a **StartSession** command, then a **GetPhotoStatus**, then an **UploadPhoto**, and finally, a **MarkLastPhotoInRoll**.

## StartSession ##

The purpose of the **StartSession** command is to let the Eye-Fi card and the server authenticate each other, and to negociate the transfer mode.

Here is an example of a **StartSession** message sent by an Eye-Fi card:

```
<?xml version="1.0" encoding="UTF-8"?>
<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ns1="EyeFi/SOAP/EyeFilm">
  <SOAP-ENV:Body>
    <ns1:StartSession>
      <macaddress>001856417729</macaddress>
      <cnonce>8744904b7ea202439631c67186690a1e</cnonce>
      <transfermode>2</transfermode>
      <transfermodetimestamp>1304505230</transfermodetimestamp>
    </ns1:StartSession>
  </SOAP-ENV:Body>
</SOAP-ENV:Envelope>
```

  * **macaddress** identifies the Eye-Fi card. The server will search in it's registry an Eye-Fi card with that MAC address. The registry contains all the information needed to continue the dialog. The upload key for instance.
  * **cnonce** is a random array of bytes that the Eye-Fi card has generated, and that is used by the authentication mechanism, as we will see below.
  * I don't know what **transfermode** and **transfermodetimestamp** are, Sceye-Fi simply sends back the same values to the Eye-Fi card.

The server will respond to such a message with a **StartSessionResponse**:

```
<?xml version="1.0" encoding="UTF-8"?>
<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
  <SOAP-ENV:Body>
    <ns1:StartSessionResponse xmlns:ns1="http://localhost/api/soap/eyefilm">
      <credential>0d400f69ce6096c3771f9465c6123145</credential>
      <snonce>d5b2b8dd7a681cfb5320aaac2fd9bba4</snonce>
      <transfermode>2</transfermode>
      <transfermodetimestamp>1304505230</transfermodetimestamp>
      <upsyncallowed>false</upsyncallowed>
    </ns1:StartSessionResponse>
  </SOAP-ENV:Body>
</SOAP-ENV:Envelope>
```

  * **credential** is the MD5 digest of **macaddress**, **cnonce**, and the upload key. It is used by the Eye-Fi card to authenticate the server.
  * **snonce** is a random array of bytes that the server has generated, and that is used by the authentication mechanism, as we will see below.
  * **transfermode** and **transfermodetimestamp** are copied from the request.
  * I don't know what **upsyncallowed** is, Sceye-Fi always set it to `false`.

## GetPhotoStatus ##

Before the Eye-Fi card starts the actual upload, it sens a **GetPhotoStatus** request:

```
<?xml version="1.0" encoding="UTF-8"?>
<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ns1="EyeFi/SOAP/EyeFilm">
  <SOAP-ENV:Body>
    <ns1:GetPhotoStatus>
      <credential>2d7b2e9f6755a1a51152a32a2d4e98a6</credential>
      <macaddress>001856417729</macaddress>
      <filename>P1030007.JPG.tar</filename>
      <filesize>1269760</filesize>
      <filesignature>343afd9e4e84d3d4f5969cd97214f7f2</filesignature>
      <flags>4</flags>
    </ns1:GetPhotoStatus>
  </SOAP-ENV:Body>
</SOAP-ENV:Envelope>
```

  * **credential** this time, it's the Eye-Fi card that is sending a credential, so that the server can authentify it. It's the MD5 digest of **macaddress**, the upload key, and the **snonce** that was sent by the server in its respond to the **StartSession** command.
  * **macaddress** is the card's MAC address of the Eye-Fi card
  * **filename** is the name of the file that the card wants to send. Note that it is a TAR file.
  * **filesize** is the size of that file
  * I don't know what the **filesignature** and the **flags** are. Sceye-Fi ignores them.

The server will respond with a **GetPhotoStatusResponse** message:

```
<?xml version="1.0" encoding="UTF-8"?>
<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
  <SOAP-ENV:Body>
    <ns1:GetPhotoStatusResponse xmlns:ns1="http://localhost/api/soap/eyefilm">
      <fileid>1</fileid>
      <offset>0</offset>
    </ns1:GetPhotoStatusResponse>
  </SOAP-ENV:Body>
</SOAP-ENV:Envelope>
```

  * **fileid** is a unique number for the file about to be send. It will be repeated by the Eye-Fi card in the forthcoming **UploadPhoto** message.
  * I don't know what **offset** means, but I guess it's a way for the server to resume an upload that was aborted. Sceye-Fi is always sending a value of 0 though, which means that if an upload is aborted, the whole file must be resent.

## UploadPhoto ##

The actual file content is sent by the Eye-Fi card in a multipart POST on path `/api/soap/eyefilm/v1/upload`. The first part, named `SOAPENVELOPE` contains the SOAP envelope for the command; the second part, named `FILENAME` contains the actual file content, and the third and last part, named `INTEGRITYDIGEST` is a 16-byte hash that can be used to check the file's integrity.

A soap envelope looks like this:

```
<?xml version="1.0" encoding="UTF-8"?>
<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ns1="EyeFi/SOAP/EyeFilm">
  <SOAP-ENV:Body>
    <ns1:UploadPhoto>
      <fileid>1</fileid>
      <macaddress>001856417729</macaddress>
      <filename>P1030007.JPG.tar</filename>
      <filesize>1269760</filesize>
      <filesignature>c8340300c434030000000000dced0300</filesignature>
      <encryption>none</encryption>
      <flags>4</flags>
    </ns1:UploadPhoto>
  </SOAP-ENV:Body>
</SOAP-ENV:Envelope>
```

  * **macaddress** is the card's MAC address of the Eye-Fi card
  * **filename** is the name of the file
  * **filesize** is the size of that file
  * I don't know what the **filesignature**, the **encryption** and the **flags** are, Sceye-Fi ignores them.


The server will respond with a **UploadPhotoResponse** message:

```
<?xml version="1.0" encoding="UTF-8"?>
<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
  <SOAP-ENV:Body>
    <ns1:UploadPhotoResponse xmlns:ns1="http://localhost/api/soap/eyefilm">
      <success>true</success>
    </ns1:UploadPhotoResponse>
  </SOAP-ENV:Body>
</SOAP-ENV:Envelope>
```

### Integrity check ###

The third part, `INTEGRITYDIGEST` contains an hexadecimal-encoded hash code that is the MD5 digest of the TCP checksums of each 512-byte block in the file content, followed by the upload key. The file being a TAR file, it is always 512-bytes padded.

The TCP checksum of a block is computed as follows:

  * the block is treated as an array of 256 16-bit unsigned integers, low-order byte first (little endian), and all these integers are added into a 32-bit sum.
  * if the 32-bit sum is greater that 65535 (2^16-1), add the high-order and low-order 16-bit words, and repeat the process while the result is greater than 65535.
  * take the complement to 1 of the result (invert all bits).

## MarkLastPhotoInRoll ##

The Eye-Fi card may upload several files in parallel. When everything is done, it sends a **MarkLastPhotoInRoll** command:

```
<?xml version="1.0" encoding="UTF-8"?>
<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ns1="EyeFi/SOAP/EyeFilm">
  <SOAP-ENV:Body>
    <ns1:MarkLastPhotoInRoll>
      <macaddress>001856417729</macaddress>
      <mergedelta>0</mergedelta>
    </ns1:MarkLastPhotoInRoll>
  </SOAP-ENV:Body>
</SOAP-ENV:Envelope>
```

  * **macaddress** as always, the MAC address of the Eye-Fi card
  * I don't know what the **mergedelta** is.

To which the server responds with a **MarkLastPhotoInRollResponse** message:

```
<?xml version="1.0" encoding="UTF-8"?>
<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
  <SOAP-ENV:Body>
    <ns1:MarkLastPhotoInRollResponse xmlns:ns1="http://localhost/api/soap/eyefilm" />
  </SOAP-ENV:Body>
</SOAP-ENV:Envelope>
```