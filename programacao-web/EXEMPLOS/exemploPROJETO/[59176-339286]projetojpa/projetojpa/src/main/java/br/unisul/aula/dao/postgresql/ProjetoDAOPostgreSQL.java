package br.unisul.aula.dao.postgresql;

import br.unisul.aula.dao.BaseDados;
import br.unisul.aula.dao.ProjetoDAO;
import br.unisul.aula.modelo.Participante;
import br.unisul.aula.modelo.Projeto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProjetoDAOPostgreSQL implements ProjetoDAO {
    @Override
    public boolean save(Projeto projeto) {
        boolean resultado = false;
        ConexaoPostgreSQL minhaConexaoPostgreSQL = new ConexaoPostgreSQL(BaseDados.POSTGRESQL);
        minhaConexaoPostgreSQL.conectar();
        Connection conn = minhaConexaoPostgreSQL.getConnection();
        PreparedStatement pstm = null;
        try {
            String sql = null;
            if (projeto.getIdProjeto() > 0) {
                sql = "update projeto set \"nmDescricao\" = ?,\"dtInicio\" = ?, \"dtFinal\" = ?," +
                        "\"nrPercConcluido\" = ?,\"nmSituacao\" = ? where \"idProjetos\" = ?";
            } else {
                sql = "insert INTO projeto (\"idProjetos\", \"nmDescricao\", \"dtInicio\", \"dtFinal\", \"nrPercConcluido\", \"nmSituacao\") " +
                        "VALUES (nextval('seq_projeto'),?,?,?,?,?)";
            }
            pstm = conn.prepareStatement(sql);
            pstm.setString(1, projeto.getDescricao());
            pstm.setDate(2,
                    new java.sql.Date(
                            projeto.getDataInicio().
                                    getTime()));
            pstm.setDate(3, new java.sql.Date(
                    projeto.getDataFim().getTime()
            ));
            pstm.setInt(4, projeto.getPercentualConcluido());
            pstm.setString(5,
                    projeto.getSituacao());
            if (projeto.getIdProjeto() > 0) {
                pstm.setInt(6, projeto.getIdProjeto());
                saveParticipantes(projeto);
                pstm.executeUpdate();

            } else {
                pstm.executeUpdate();
                ResultSet rsId = pstm.getGeneratedKeys();
                if (rsId.next()) projeto.setIdProjeto(rsId.getInt("idProjetos"));
                saveParticipantes(projeto);

            }




            resultado = true;

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pstm != null) {
                try {
                    pstm.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        return resultado;
    }

    private void saveParticipantes(Projeto projeto) {
        boolean resultado = false;
        ConexaoPostgreSQL minhaConexaoPostgreSQL = new ConexaoPostgreSQL(BaseDados.POSTGRESQL);
        minhaConexaoPostgreSQL.conectar();
        Connection conn = minhaConexaoPostgreSQL.getConnection();
        PreparedStatement pstm = null;
        try {
            String sqlDel = "delete from controles where \"idProjetos\" = ?";
            PreparedStatement pstmDel =
                    conn.prepareStatement(sqlDel);
            pstmDel.setInt(1, projeto.getIdProjeto());
            pstmDel.executeUpdate();

            for (Participante participante : projeto.getParticipantes()) {
                String sql = "insert into " +
                        "controles (\"idProjetos\", " +
                        "\"idParticipantes\") values( ? , ? )";
                pstm = conn.prepareStatement(sql);
                pstm.setInt(1, projeto.getIdProjeto());
                pstm.setInt(2, participante.getIdParticipante());
                System.out.println("aqui:"+sql);
                pstm.executeUpdate();
                resultado = true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pstm != null) {
                try {
                    pstm.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public Projeto findById(int idProjeto) {
        boolean resultado = false;
        ConexaoPostgreSQL minhaConexaoPostgreSQL = new ConexaoPostgreSQL(BaseDados.POSTGRESQL);
        minhaConexaoPostgreSQL.conectar();
        Connection conn = minhaConexaoPostgreSQL.getConnection();
        PreparedStatement pstm = null;
        ResultSet rs = null;
        Projeto projeto = null;
        try {
            String sql = "select \"idProjetos\",\"nmDescricao\"," +
                    "TO_CHAR(\"dtInicio\",'YYYY-MM-DD') as \"dtInicio\"," +
                    "TO_CHAR(\"dtFinal\",'YYYY-MM-DD') as \"dtFinal\"," +
                    "\"nrPercConcluido\", \"nmSituacao\" from projeto \n" +
                    "where \"idProjetos\" = ?";
            pstm = conn.prepareStatement(sql);
            pstm.setInt(1, idProjeto);
            rs = pstm.executeQuery();
            while (rs.next()) {
                projeto = new Projeto();
                projeto.setIdProjeto(rs.getInt("idProjetos"));
                projeto.setDescricao(rs.getString("nmDescricao"));
                projeto.setDataInicio(rs.getString("dtInicio"));
                projeto.setDataFim(rs.getString("dtFinal"));
                projeto.setPercentualConcluido(rs.getInt("nrPercConcluido"));
                projeto.setSituacao(rs.getString("nmSituacao"));

            }
            this.carregarParticipantes(projeto);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return projeto;
    }

    private boolean carregarParticipantes(Projeto projeto) {
        boolean resultado = false;
        Participante participante = null;
        ConexaoPostgreSQL minhaConexaoPostgreSQL = new ConexaoPostgreSQL(BaseDados.POSTGRESQL);
        minhaConexaoPostgreSQL.conectar();
        Connection conn = minhaConexaoPostgreSQL.getConnection();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            String sql = "select p.\"idParticipantes\", " +
                    "p.\"nmParticipante\", " +
                    "p.\"nmCargo\", " +
                    "c.\"idProjetos\" " +
                    "from participantes p, " +
                    "controles c " +
                    "where " +
                    "p.\"idParticipantes\" = c.\"idParticipantes\" " +
                    "and c.\"idProjetos\" = ? ";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, projeto.getIdProjeto());
            rs = pstmt.executeQuery();
            participante = new Participante();
            while (rs.next()) {
                participante.setIdParticipante(rs.getInt("idParticipantes"));
                participante.setNome(rs.getString("nmParticipante"));
                participante.setCargo(rs.getString("nmCargo"));
                projeto.getParticipantes().add(participante);
            }
            resultado = true;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return resultado;

    }

    @Override
    public List<Projeto> findAll() {
        Projeto projeto = null;
        ArrayList<Projeto> lista = new ArrayList<Projeto>();
        ConexaoPostgreSQL minhaConexaoPostgreSQL = new ConexaoPostgreSQL(BaseDados.POSTGRESQL);
        minhaConexaoPostgreSQL.conectar();
        Connection conn = minhaConexaoPostgreSQL.getConnection();
        PreparedStatement pstm = null;
        ResultSet rs = null;
        try {
            String sql = "select p.\"idProjetos\", p.\"nmDescricao\", to_char(p.\"dtInicio\",'YYYY-MM-DD') AS \"dtInicio\", " +
                    " to_char(p.\"dtFinal\",'YYYY-MM-DD') as \"dtFinal\", p.\"nrPercConcluido\", p.\"nmSituacao\" " +
                    "from projeto p";
            pstm = conn.prepareStatement(sql);
            rs = pstm.executeQuery();
            while (rs.next()) {
                projeto = new Projeto();
                projeto.setIdProjeto(rs.getInt("idProjetos"));
                projeto.setDescricao(rs.getString("nmDescricao"));
                projeto.setDataInicio(rs.getString("dtInicio"));
                projeto.setDataFim(rs.getString("dtFinal"));
                projeto.setPercentualConcluido(rs.getInt("nrPercConcluido"));
                projeto.setSituacao(rs.getString("nmSituacao"));
                carregarParticipantes(projeto);
                lista.add(projeto);

            }
            this.carregarParticipantes(projeto);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    @Override
    public boolean delete(int idProjeto) {
        return false;
    }

    @Override
    public void update(Projeto projeto){}
}
