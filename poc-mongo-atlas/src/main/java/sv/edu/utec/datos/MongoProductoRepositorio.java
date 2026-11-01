package sv.edu.utec.datos;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import sv.edu.utec.Modelo.Producto;
import sv.edu.utec.Modelo.ResumenCategoria;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class MongoProductoRepositorio implements ProductoRepositorio {
    private final MongoCollection<Document> coleccion;
    public MongoProductoRepositorio(MongoDatabase bd) {
        this.coleccion = bd.getCollection("productos");
        coleccion.createIndex(Indexes.ascending("codigo"),
                new IndexOptions().unique(true));
        coleccion.createIndex(Indexes.ascending("categoria"));
    }

    @Override
    public void insertar(Producto p) {
        try {
            coleccion.insertOne(aDocumento(p));
        } catch (MongoWriteException e) {
            if (e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
                throw new RepositorioException(
                        "Ya existe un producto con el codigo " + p.getCodigo(), e);
            }
            throw e;
        }
    }
    @Override
    public int insertarVarios(List<Producto> productos) {
        if (productos.isEmpty()) {
            return 0; // insertMany no acepta listas vacias
        }
        List<Document> documentos = new ArrayList<>();
        for (Producto p : productos) {
            documentos.add(aDocumento(p));
        }
        try {
            return coleccion.insertMany(documentos).getInsertedIds().size();
        } catch (MongoBulkWriteException e) {
            throw new RepositorioException("Carga detenida: "
                    + e.getWriteResult().getInsertedCount()
                    + " insertados antes de un codigo duplicado", e);
        }
    }

    @Override
    public Optional<Producto> buscarPorCodigo(String codigo) {
        Document d = coleccion.find(Filters.eq("codigo", codigo)).first();
        return Optional.ofNullable(d).map(this::aProducto);
    }
    @Override
    public List<Producto> listarTodos() {
        return buscar(new Document(), Sorts.ascending("codigo"));
    }
    /** Ejecuta un find con filtro y orden, y convierte cada documento. */
    private List<Producto> buscar(Bson filtro, Bson orden) {
        List<Producto> resultado = new ArrayList<>();
        for (Document d : coleccion.find(filtro).sort(orden)) {
            resultado.add(aProducto(d));
        }
        return resultado;
    }

    @Override
    public List<Producto> listarPorCategoria(String categoria) {
        return List.of();
    }

    @Override
    public List<Producto> buscarPorNombre(String texto) {
        return List.of();
    }

    @Override
    public List<Producto> buscarPorRangoDePrecio(double minimo, double maximo) {
        return List.of();
    }

    @Override
    public boolean actualizar(Producto producto) {
        return false;
    }

    @Override
    public boolean ajustarExistencia(String codigo, int cantidad) {
        return false;
    }

    @Override
    public boolean eliminar(String codigo) {
        DeleteResult r = coleccion.deleteOne(Filters.eq("codigo", codigo));
        return r.getDeletedCount() > 0;
    }

    @Override
    public List<ResumenCategoria> resumenPorCategoria() {
        return List.of();
    }

    private Document aDocumento(Producto p) {
        return new Document("codigo", p.getCodigo())
                .append("nombre", p.getNombre())
                .append("categoria", p.getCategoria())
                .append("precio", p.getPrecio())
                .append("existencia", p.getExistencia())
                .append("creadoEn", new Date());
    }
    private Producto aProducto(Document d) {
        return new Producto(
                d.getString("codigo"),
                d.getString("nombre"),
                d.getString("categoria"),
                d.get("precio", Number.class).doubleValue(),
                d.get("existencia", Number.class).intValue());
    }
}