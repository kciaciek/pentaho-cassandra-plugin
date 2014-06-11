/*******************************************************************************
 *
 * Pentaho Big Data
 *
 * Copyright (C) 2002-2013 by Pentaho : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.pentaho.di.trans.steps.cassandraoutput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cassandra.thrift.InvalidRequestException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.pentaho.cassandra.CassandraUtils;
import org.pentaho.cassandra.ConnectionFactory;
import org.pentaho.cassandra.legacy.CassandraColumnMetaData;
import org.pentaho.cassandra.spi.Connection;
import org.pentaho.cassandra.spi.Keyspace;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.Props;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.util.StringUtil;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.ui.core.dialog.EnterSelectionDialog;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.core.dialog.ShowMessageDialog;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.di.ui.trans.step.BaseStepDialog;

/**
 * Dialog class for the CassandraOutput step
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 */
public class CassandraOutputDialog extends BaseStepDialog implements StepDialogInterface {

  private static final Class<?> PKG = CassandraOutputMeta.class;

  private final CassandraOutputMeta m_currentMeta;
  private final CassandraOutputMeta m_originalMeta;

  /** various UI bits and pieces for the dialog */
  private Label m_stepnameLabel;
  private Text m_stepnameText;

  private CTabFolder m_wTabFolder;
  private CTabItem m_connectionTab;
  private CTabItem m_writeTab;
  private CTabItem m_schemaTab;

  private Label m_hostLab;
  private TextVar m_hostText;
  private Label m_portLab;
  private TextVar m_portText;

  private Label m_userLab;
  private TextVar m_userText;
  private Label m_passLab;
  private TextVar m_passText;

  private Label m_socketTimeoutLab;
  private TextVar m_socketTimeoutText;

  private Label m_keyspaceLab;
  private TextVar m_keyspaceText;

  private Label m_columnFamilyLab;
  private CCombo m_columnFamilyCombo;
  private Button m_getColumnFamiliesBut;

  private Label m_consistencyLab;
  private TextVar m_consistencyText;

  private Label m_batchSizeLab;
  private TextVar m_batchSizeText;

  private Label m_batchInsertTimeoutLab;
  private TextVar m_batchInsertTimeoutText;
  private Label m_subBatchSizeLab;
  private TextVar m_subBatchSizeText;

  private Label m_unloggedBatchLab;
  private Button m_unloggedBatchBut;

  private Label m_keyFieldLab;
  private CCombo m_keyFieldCombo;

  private Button m_getFieldsBut;

  private Label m_useCQL3Lab;
  private Button m_useCQL3Check;

  private Label m_useThriftIOLab;
  private Button m_useThriftIOCheck;

  private Label m_schemaHostLab;
  private TextVar m_schemaHostText;
  private Label m_schemaPortLab;
  private TextVar m_schemaPortText;

  private Label m_createColumnFamilyLab;
  private Button m_createColumnFamilyBut;

  private Label m_withClauseLab;
  private TextVar m_withClauseText;

  private Label m_truncateColumnFamilyLab;
  private Button m_truncateColumnFamilyBut;

  private Label m_updateColumnFamilyMetaDataLab;
  private Button m_updateColumnFamilyMetaDataBut;

  private Label m_insertFieldsNotInColumnFamMetaLab;
  private Button m_insertFieldsNotInColumnFamMetaBut;

  private Label m_compressionLab;
  private Button m_useCompressionBut;

  private Button m_showSchemaBut;

  private Button m_aprioriCQLBut;

  private String m_aprioriCQL;
  private boolean m_dontComplain;

  private CCombo m_ttlUnitsCombo;
  private TextVar m_ttlValueText;

  public CassandraOutputDialog( Shell parent, Object in, TransMeta tr, String name ) {

    super( parent, (BaseStepMeta) in, tr, name );

    m_currentMeta = (CassandraOutputMeta) in;
    m_originalMeta = (CassandraOutputMeta) m_currentMeta.clone();
  }

  @Override
  public String open() {

    Shell parent = getParent();
    Display display = parent.getDisplay();

    shell = new Shell( parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN | SWT.MAX );

    props.setLook( shell );
    setShellImage( shell, m_currentMeta );

    // used to listen to a text field (m_wStepname)
    final ModifyListener lsMod = new ModifyListener() {
      @Override
      public void modifyText( ModifyEvent e ) {
        m_currentMeta.setChanged();
      }
    };

    changed = m_currentMeta.hasChanged();

    FormLayout formLayout = new FormLayout();
    formLayout.marginWidth = Const.FORM_MARGIN;
    formLayout.marginHeight = Const.FORM_MARGIN;

    shell.setLayout( formLayout );
    shell.setText( BaseMessages.getString( PKG, "CassandraOutputDialog.Shell.Title" ) ); //$NON-NLS-1$

    int middle = props.getMiddlePct();
    int margin = Const.MARGIN;

    // Stepname line
    m_stepnameLabel = new Label( shell, SWT.RIGHT );
    m_stepnameLabel.setText( BaseMessages.getString( PKG, "CassandraOutputDialog.StepName.Label" ) ); //$NON-NLS-1$
    props.setLook( m_stepnameLabel );

    FormData fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.right = new FormAttachment( middle, -margin );
    fd.top = new FormAttachment( 0, margin );
    m_stepnameLabel.setLayoutData( fd );
    m_stepnameText = new Text( shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    m_stepnameText.setText( stepname );
    props.setLook( m_stepnameText );
    m_stepnameText.addModifyListener( lsMod );

    // format the text field
    fd = new FormData();
    fd.left = new FormAttachment( middle, 0 );
    fd.top = new FormAttachment( 0, margin );
    fd.right = new FormAttachment( 100, 0 );
    m_stepnameText.setLayoutData( fd );

    m_wTabFolder = new CTabFolder( shell, SWT.BORDER );
    props.setLook( m_wTabFolder, Props.WIDGET_STYLE_TAB );
    m_wTabFolder.setSimple( false );

    // start of the connection tab
    m_connectionTab = new CTabItem( m_wTabFolder, SWT.BORDER );
    m_connectionTab.setText( BaseMessages.getString( PKG, "CassandraOutputDialog.Tab.Connection" ) ); //$NON-NLS-1$

    Composite wConnectionComp = new Composite( m_wTabFolder, SWT.NONE );
    props.setLook( wConnectionComp );

    FormLayout connectionLayout = new FormLayout();
    connectionLayout.marginWidth = 3;
    connectionLayout.marginHeight = 3;
    wConnectionComp.setLayout( connectionLayout );

    // host line
    m_hostLab = new Label( wConnectionComp, SWT.RIGHT );
    props.setLook( m_hostLab );
    m_hostLab.setText( BaseMessages.getString( PKG, "CassandraOutputDialog.Hostname.Label" ) ); //$NON-NLS-1$
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.top = new FormAttachment( m_stepnameText, margin );
    fd.right = new FormAttachment( middle, -margin );
    m_hostLab.setLayoutData( fd );

    m_hostText = new TextVar( transMeta, wConnectionComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( m_hostText );
    m_hostText.addModifyListener( new ModifyListener() {
      @Override
      public void modifyText( ModifyEvent e ) {
        m_hostText.setToolTipText( transMeta.environmentSubstitute( m_hostText.getText() ) );
      }
    } );
    m_hostText.addModifyListener( lsMod );
    fd = new FormData();
    fd.right = new FormAttachment( 100, 0 );
    fd.top = new FormAttachment( m_stepnameText, margin );
    fd.left = new FormAttachment( middle, 0 );
    m_hostText.setLayoutData( fd );

    // port line
    m_portLab = new Label( wConnectionComp, SWT.RIGHT );
    props.setLook( m_portLab );
    m_portLab.setText( BaseMessages.getString( PKG, "CassandraOutputDialog.Port.Label" ) ); //$NON-NLS-1$
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.top = new FormAttachment( m_hostText, margin );
    fd.right = new FormAttachment( middle, -margin );
    m_portLab.setLayoutData( fd );

    m_portText = new TextVar( transMeta, wConnectionComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( m_portText );
    m_portText.addModifyListener( new ModifyListener() {
      @Override
      public void modifyText( ModifyEvent e ) {
        m_portText.setToolTipText( transMeta.environmentSubstitute( m_portText.getText() ) );
      }
    } );
    m_portText.addModifyListener( lsMod );
    fd = new FormData();
    fd.right = new FormAttachment( 100, 0 );
    fd.top = new FormAttachment( m_hostText, margin );
    fd.left = new FormAttachment( middle, 0 );
    m_portText.setLayoutData( fd );

    // socket timeout line
    m_socketTimeoutLab = new Label( wConnectionComp, SWT.RIGHT );
    props.setLook( m_socketTimeoutLab );
    m_socketTimeoutLab.setText( BaseMessages.getString( PKG, "CassandraOutputDialog.SocketTimeout.Label" ) ); //$NON-NLS-1$
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.top = new FormAttachment( m_portText, margin );
    fd.right = new FormAttachment( middle, -margin );
    m_socketTimeoutLab.setLayoutData( fd );

    m_socketTimeoutText = new TextVar( transMeta, wConnectionComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( m_socketTimeoutText );
    m_socketTimeoutText.addModifyListener( new ModifyListener() {
      @Override
      public void modifyText( ModifyEvent e ) {
        m_socketTimeoutText.setToolTipText( transMeta.environmentSubstitute( m_socketTimeoutText.getText() ) );
      }
    } );
    m_socketTimeoutText.addModifyListener( lsMod );
    fd = new FormData();
    fd.right = new FormAttachment( 100, 0 );
    fd.top = new FormAttachment( m_portText, margin );
    fd.left = new FormAttachment( middle, 0 );
    m_socketTimeoutText.setLayoutData( fd );

    // username line
    m_userLab = new Label( wConnectionComp, SWT.RIGHT );
    props.setLook( m_userLab );
    m_userLab.setText( BaseMessages.getString( PKG, "CassandraOutputDialog.User.Label" ) ); //$NON-NLS-1$
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.top = new FormAttachment( m_socketTimeoutText, margin );
    fd.right = new FormAttachment( middle, -margin );
    m_userLab.setLayoutData( fd );

    m_userText = new TextVar( transMeta, wConnectionComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( m_userText );
    m_userText.addModifyListener( new ModifyListener() {
      @Override
      public void modifyText( ModifyEvent e ) {
        m_userText.setToolTipText( transMeta.environmentSubstitute( m_userText.getText() ) );
      }
    } );
    m_userText.addModifyListener( lsMod );
    fd = new FormData();
    fd.right = new FormAttachment( 100, 0 );
    fd.top = new FormAttachment( m_socketTimeoutText, margin );
    fd.left = new FormAttachment( middle, 0 );
    m_userText.setLayoutData( fd );

    // password line
    m_passLab = new Label( wConnectionComp, SWT.RIGHT );
    props.setLook( m_passLab );
    m_passLab.setText( BaseMessages.getString( PKG, "CassandraOutputDialog.Password.Label" ) ); //$NON-NLS-1$
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.top = new FormAttachment( m_userText, margin );
    fd.right = new FormAttachment( middle, -margin );
    m_passLab.setLayoutData( fd );

    m_passText = new TextVar( transMeta, wConnectionComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( m_passText );
    m_passText.setEchoChar( '*' );
    // If the password contains a variable, don't hide it.
    m_passText.getTextWidget().addModifyListener( new ModifyListener() {
      @Override
      public void modifyText( ModifyEvent e ) {
        checkPasswordVisible();
      }
    } );

    m_passText.addModifyListener( lsMod );

    fd = new FormData();
    fd.right = new FormAttachment( 100, 0 );
    fd.top = new FormAttachment( m_userText, margin );
    fd.left = new FormAttachment( middle, 0 );
    m_passText.setLayoutData( fd );

    // keyspace line
    m_keyspaceLab = new Label( wConnectionComp, SWT.RIGHT );
    props.setLook( m_keyspaceLab );
    m_keyspaceLab.setText( BaseMessages.getString( PKG, "CassandraOutputDialog.Keyspace.Label" ) ); //$NON-NLS-1$
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.top = new FormAttachment( m_passText, margin );
    fd.right = new FormAttachment( middle, -margin );
    m_keyspaceLab.setLayoutData( fd );

    m_keyspaceText = new TextVar( transMeta, wConnectionComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( m_keyspaceText );
    m_keyspaceText.addModifyListener( new ModifyListener() {
      @Override
      public void modifyText( ModifyEvent e ) {
        m_keyspaceText.setToolTipText( transMeta.environmentSubstitute( m_keyspaceText.getText() ) );
      }
    } );
    m_keyspaceText.addModifyListener( lsMod );
    fd = new FormData();
    fd.right = new FormAttachment( 100, 0 );
    fd.top = new FormAttachment( m_passText, margin );
    fd.left = new FormAttachment( middle, 0 );
    m_keyspaceText.setLayoutData( fd );

    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.top = new FormAttachment( 0, 0 );
    fd.right = new FormAttachment( 100, 0 );
    fd.bottom = new FormAttachment( 100, 0 );
    wConnectionComp.setLayoutData( fd );

    wConnectionComp.layout();
    m_connectionTab.setControl( wConnectionComp );

    // --- start of the write tab ---
    m_writeTab = new CTabItem( m_wTabFolder, SWT.NONE );
    m_writeTab.setText( BaseMessages.getString( PKG, "CassandraOutputDialog.Tab.Write" ) ); //$NON-NLS-1$
    Composite wWriteComp = new Composite( m_wTabFolder, SWT.NONE );
    props.setLook( wWriteComp );

    FormLayout writeLayout = new FormLayout();
    writeLayout.marginWidth = 3;
    writeLayout.marginHeight = 3;
    wWriteComp.setLayout( writeLayout );

    // column family line
    m_columnFamilyLab = new Label( wWriteComp, SWT.RIGHT );
    props.setLook( m_columnFamilyLab );
    m_columnFamilyLab.setText( BaseMessages.getString( PKG, "CassandraOutputDialog.ColumnFamily.Label" ) ); //$NON-NLS-1$
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.top = new FormAttachment( 0, 0 );
    fd.right = new FormAttachment( middle, -margin );
    m_columnFamilyLab.setLayoutData( fd );

    m_getColumnFamiliesBut = new Button( wWriteComp, SWT.PUSH | SWT.CENTER );
    props.setLook( m_getColumnFamiliesBut );
    m_getColumnFamiliesBut.setText( BaseMessages.getString( PKG, "CassandraOutputDialog.GetColFam.Button" ) ); //$NON-NLS-1$
    fd = new FormData();
    fd.right = new FormAttachment( 100, 0 );
    fd.top = new FormAttachment( 0, 0 );
    m_getColumnFamiliesBut.setLayoutData( fd );
    m_getColumnFamiliesBut.addSelectionListener( new SelectionAdapter() {
      @Override
      public void widgetSelected( SelectionEvent e ) {
        setupColumnFamiliesCombo();
      }
    } );

    m_columnFamilyCombo = new CCombo( wWriteComp, SWT.BORDER );
    props.setLook( m_columnFamilyCombo );
    m_columnFamilyCombo.addModifyListener( new ModifyListener() {
      @Override
      public void modifyText( ModifyEvent e ) {
        m_columnFamilyCombo.setToolTipText( transMeta.environmentSubstitute( m_columnFamilyCombo.getText() ) );
      }
    } );
    m_columnFamilyCombo.addModifyListener( lsMod );
    fd = new FormData();
    fd.right = new FormAttachment( m_getColumnFamiliesBut, -margin );
    fd.top = new FormAttachment( 0, margin );
    fd.left = new FormAttachment( middle, 0 );
    m_columnFamilyCombo.setLayoutData( fd );

    // consistency line
    m_consistencyLab = new Label( wWriteComp, SWT.RIGHT );
    props.setLook( m_consistencyLab );
    m_consistencyLab.setText( BaseMessages.getString( PKG, "CassandraOutputDialog.Consistency.Label" ) ); //$NON-NLS-1$
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.top = new FormAttachment( m_columnFamilyCombo, margin );
    fd.right = new FormAttachment( middle, -margin );
    m_consistencyLab.setLayoutData( fd );
    m_consistencyLab.setToolTipText( BaseMessages.getString( PKG, "CassandraOutputDialog.Consistency.Label.TipText" ) ); //$NON-NLS-1$

    m_consistencyText = new TextVar( transMeta, wWriteComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( m_consistencyText );
    m_consistencyText.addModifyListener( new ModifyListener() {
      @Override
      public void modifyText( ModifyEvent e ) {
        m_consistencyText.setToolTipText( transMeta.environmentSubstitute( m_consistencyText.getText() ) );
      }
    } );
    m_consistencyText.addModifyListener( lsMod );
    fd = new FormData();
    fd.right = new FormAttachment( 100, 0 );
    fd.top = new FormAttachment( m_columnFamilyCombo, margin );
    fd.left = new FormAttachment( middle, 0 );
    m_consistencyText.setLayoutData( fd );

    // batch size line
    m_batchSizeLab = new Label( wWriteComp, SWT.RIGHT );
    props.setLook( m_batchSizeLab );
    m_batchSizeLab.setText( BaseMessages.getString( PKG, "CassandraOutputDialog.BatchSize.Label" ) ); //$NON-NLS-1$
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.top = new FormAttachment( m_consistencyText, margin );
    fd.right = new FormAttachment( middle, -margin );
    m_batchSizeLab.setLayoutData( fd );
    m_batchSizeLab.setToolTipText( BaseMessages.getString( PKG, "CassandraOutputDialog.BatchSize.TipText" ) ); //$NON-NLS-1$

    m_batchSizeText = new TextVar( transMeta, wWriteComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( m_batchSizeText );
    m_batchSizeText.addModifyListener( new ModifyListener() {
      @Override
      public void modifyText( ModifyEvent e ) {
        m_batchSizeText.setToolTipText( transMeta.environmentSubstitute( m_batchSizeText.getText() ) );
      }
    } );
    m_batchSizeText.addModifyListener( lsMod );
    fd = new FormData();
    fd.right = new FormAttachment( 100, 0 );
    fd.top = new FormAttachment( m_consistencyText, margin );
    fd.left = new FormAttachment( middle, 0 );
    m_batchSizeText.setLayoutData( fd );

    // batch insert timeout
    m_batchInsertTimeoutLab = new Label( wWriteComp, SWT.RIGHT );
    props.setLook( m_batchInsertTimeoutLab );
    m_batchInsertTimeoutLab.setText( BaseMessages.getString( PKG, "CassandraOutputDialog.BatchInsertTimeout.Label" ) ); //$NON-NLS-1$
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.top = new FormAttachment( m_batchSizeText, margin );
    fd.right = new FormAttachment( middle, -margin );
    m_batchInsertTimeoutLab.setLayoutData( fd );
    m_batchInsertTimeoutLab.setToolTipText( BaseMessages.getString( PKG,
        "CassandraOutputDialog.BatchInsertTimeout.TipText" ) ); //$NON-NLS-1$

    m_batchInsertTimeoutText = new TextVar( transMeta, wWriteComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( m_batchInsertTimeoutText );
    m_batchInsertTimeoutText.addModifyListener( new ModifyListener() {
      @Override
      public void modifyText( ModifyEvent e ) {
        m_batchInsertTimeoutText.setToolTipText( transMeta.environmentSubstitute( m_batchInsertTimeoutText.getText() ) );
      }
    } );
    m_batchInsertTimeoutText.addModifyListener( lsMod );
    fd = new FormData();
    fd.right = new FormAttachment( 100, 0 );
    fd.top = new FormAttachment( m_batchSizeText, margin );
    fd.left = new FormAttachment( middle, 0 );
    m_batchInsertTimeoutText.setLayoutData( fd );

    // sub-batch size
    m_subBatchSizeLab = new Label( wWriteComp, SWT.RIGHT );
    props.setLook( m_subBatchSizeLab );
    m_subBatchSizeLab.setText( BaseMessages.getString( PKG, "CassandraOutputDialog.SubBatchSize.Label" ) ); //$NON-NLS-1$
    m_subBatchSizeLab.setToolTipText( BaseMessages.getString( PKG, "CassandraOutputDialog.SubBatchSize.TipText" ) ); //$NON-NLS-1$
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.top = new FormAttachment( m_batchInsertTimeoutText, margin );
    fd.right = new FormAttachment( middle, -margin );
    m_subBatchSizeLab.setLayoutData( fd );

    m_subBatchSizeText = new TextVar( transMeta, wWriteComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( m_subBatchSizeText );
    m_subBatchSizeText.addModifyListener( new ModifyListener() {
      @Override
      public void modifyText( ModifyEvent e ) {
        m_subBatchSizeText.setToolTipText( transMeta.environmentSubstitute( m_subBatchSizeText.getText() ) );
      }
    } );
    m_subBatchSizeText.addModifyListener( lsMod );
    fd = new FormData();
    fd.right = new FormAttachment( 100, 0 );
    fd.top = new FormAttachment( m_batchInsertTimeoutText, margin );
    fd.left = new FormAttachment( middle, 0 );
    m_subBatchSizeText.setLayoutData( fd );

    // unlogged batch line
    m_unloggedBatchLab = new Label( wWriteComp, SWT.RIGHT );
    m_unloggedBatchLab.setText( BaseMessages.getString( PKG, "CassandraOutputDialog.UnloggedBatch.Label" ) ); //$NON-NLS-1$
    m_unloggedBatchLab.setToolTipText( BaseMessages.getString( PKG, "CassandraOutputDialog.UnloggedBatch.TipText" ) ); //$NON-NLS-1$
    props.setLook( m_unloggedBatchLab );
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.top = new FormAttachment( m_subBatchSizeText, margin );
    fd.right = new FormAttachment( middle, -margin );
    m_unloggedBatchLab.setLayoutData( fd );

    m_unloggedBatchBut = new Button( wWriteComp, SWT.CHECK );
    props.setLook( m_unloggedBatchBut );
    fd = new FormData();
    fd.right = new FormAttachment( 100, 0 );
    fd.top = new FormAttachment( m_subBatchSizeText, margin );
    fd.left = new FormAttachment( middle, 0 );
    m_unloggedBatchBut.setLayoutData( fd );

    // TTL line
    Label ttlLab = new Label( wWriteComp, SWT.RIGHT );
    props.setLook( ttlLab );
    ttlLab.setText( BaseMessages.getString( PKG, "CassandraOutputDialog.TTL.Label" ) ); //$NON-NLS-1$
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.top = new FormAttachment( m_unloggedBatchBut, margin );
    fd.right = new FormAttachment( middle, -margin );
    ttlLab.setLayoutData( fd );

    m_ttlUnitsCombo = new CCombo( wWriteComp, SWT.BORDER );
    m_ttlUnitsCombo.setEditable( false );
    props.setLook( m_ttlUnitsCombo );
    fd = new FormData();
    fd.right = new FormAttachment( 100, 0 );
    fd.top = new FormAttachment( m_unloggedBatchBut, margin );
    m_ttlUnitsCombo.setLayoutData( fd );

    for ( CassandraOutputMeta.TTLUnits u : CassandraOutputMeta.TTLUnits.values() ) {
      m_ttlUnitsCombo.add( u.toString() );
    }

    m_ttlUnitsCombo.select( 0 );

    m_ttlUnitsCombo.addSelectionListener( new SelectionAdapter() {
      @Override
      public void widgetSelected( SelectionEvent e ) {
        if ( m_ttlUnitsCombo.getSelectionIndex() == 0 ) {
          m_ttlValueText.setEnabled( false );
          m_ttlValueText.setText( "" ); //$NON-NLS-1$
        } else {
          m_ttlValueText.setEnabled( true );
        }
      }
    } );

    m_ttlValueText = new TextVar( transMeta, wWriteComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( m_ttlValueText );
    fd = new FormData();
    fd.right = new FormAttachment( m_ttlUnitsCombo, -margin );
    fd.top = new FormAttachment( m_unloggedBatchBut, margin );
    fd.left = new FormAttachment( middle, 0 );
    m_ttlValueText.setLayoutData( fd );
    m_ttlValueText.setEnabled( false );
    m_ttlValueText.addModifyListener( new ModifyListener() {
      @Override
      public void modifyText( ModifyEvent e ) {
        m_ttlValueText.setToolTipText( transMeta.environmentSubstitute( m_ttlValueText.getText() ) );
      }
    } );

    // key field line
    m_keyFieldLab = new Label( wWriteComp, SWT.RIGHT );
    props.setLook( m_keyFieldLab );
    m_keyFieldLab.setText( BaseMessages.getString( PKG, "CassandraOutputDialog.KeyField.Label" ) ); //$NON-NLS-1$
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.top = new FormAttachment( m_ttlValueText, margin );
    fd.right = new FormAttachment( middle, -margin );
    m_keyFieldLab.setLayoutData( fd );

    m_getFieldsBut = new Button( wWriteComp, SWT.PUSH | SWT.CENTER );
    props.setLook( m_getFieldsBut );
    m_getFieldsBut.setText( " " //$NON-NLS-1$
        + BaseMessages.getString( PKG, "CassandraOutputDialog.GetFields.Button" ) //$NON-NLS-1$
        + " " ); //$NON-NLS-1$
    fd = new FormData();
    fd.right = new FormAttachment( 100, 0 );
    fd.top = new FormAttachment( m_ttlValueText, margin );
    m_getFieldsBut.setLayoutData( fd );

    m_getFieldsBut.addSelectionListener( new SelectionAdapter() {
      @Override
      public void widgetSelected( SelectionEvent e ) {
        if ( m_useCQL3Check.getSelection() ) {
          showEnterSelectionDialog();
        } else {
          setupFieldsCombo();
        }
      }
    } );

    m_keyFieldCombo = new CCombo( wWriteComp, SWT.BORDER );
    m_keyFieldCombo.addModifyListener( new ModifyListener() {
      @Override
      public void modifyText( ModifyEvent e ) {
        m_keyFieldCombo.setToolTipText( transMeta.environmentSubstitute( m_keyFieldCombo.getText() ) );
      }
    } );
    m_keyFieldCombo.addModifyListener( lsMod );
    fd = new FormData();
    fd.right = new FormAttachment( m_getFieldsBut, -margin );
    fd.top = new FormAttachment( m_ttlValueText, margin );
    fd.left = new FormAttachment( middle, 0 );
    m_keyFieldCombo.setLayoutData( fd );

    m_useCQL3Lab = new Label( wWriteComp, SWT.RIGHT );
    props.setLook( m_useCQL3Lab );
    m_useCQL3Lab.setText( BaseMessages.getString( PKG, "CassandraOutputDialog.UseCQL3.Label" ) ); //$NON-NLS-1$
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.top = new FormAttachment( m_keyFieldCombo, margin );
    fd.right = new FormAttachment( middle, -margin );
    m_useCQL3Lab.setLayoutData( fd );

    m_useCQL3Check = new Button( wWriteComp, SWT.CHECK );
    props.setLook( m_useCQL3Check );
    fd = new FormData();
    fd.right = new FormAttachment( 100, 0 );
    fd.top = new FormAttachment( m_keyFieldCombo, margin );
    fd.left = new FormAttachment( middle, 0 );
    m_useCQL3Check.setLayoutData( fd );

    m_useCQL3Check.addSelectionListener( new SelectionAdapter() {
      @Override
      public void widgetSelected( SelectionEvent e ) {
        m_useThriftIOCheck.setEnabled( !m_useCQL3Check.getSelection() );
        m_unloggedBatchBut.setEnabled( m_useCQL3Check.getSelection() );
        if ( m_useCQL3Check.getSelection() ) {
          m_useThriftIOCheck.setSelection( false );
          m_getFieldsBut.setText( BaseMessages.getString( PKG, "CassandraOutputDialog.SelectFields.Button" ) ); //$NON-NLS-1$
          m_keyFieldLab.setText( BaseMessages.getString( PKG, "CassandraOutputDialog.KeyFields.Label" ) ); //$NON-NLS-1$
        } else {
          m_getFieldsBut.setText( " " //$NON-NLS-1$
              + BaseMessages.getString( PKG, "CassandraOutputDialog.GetFields.Button" ) + " " ); //$NON-NLS-1$ //$NON-NLS-2$
          m_keyFieldLab.setText( BaseMessages.getString( PKG, "CassandraOutputDialog.KeyField.Label" ) ); //$NON-NLS-1$
          m_unloggedBatchBut.setSelection( false );
        }
        checkWidgetsWithRespectToDriver( ConnectionFactory.Driver.LEGACY_THRIFT );
      }
    } );

    m_useThriftIOLab = new Label( wWriteComp, SWT.RIGHT );
    props.setLook( m_useThriftIOLab );
    m_useThriftIOLab.setText( BaseMessages.getString( PKG, "CassandraOutputDialog.UseThriftIO.Label" ) ); //$NON-NLS-1$
    m_useThriftIOLab.setToolTipText( BaseMessages.getString( PKG, "CassandraOutputDialog.UseThriftIO.TipText" ) ); //$NON-NLS-1$
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.top = new FormAttachment( m_useCQL3Check, margin );
    fd.right = new FormAttachment( middle, -margin );
    m_useThriftIOLab.setLayoutData( fd );

    m_useThriftIOCheck = new Button( wWriteComp, SWT.CHECK );
    m_useThriftIOCheck.setToolTipText( BaseMessages.getString( PKG, "CassandraOutputDialog.UseThriftIO.TipText" ) ); //$NON-NLS-1$
    props.setLook( m_useThriftIOCheck );
    fd = new FormData();
    fd.right = new FormAttachment( 100, 0 );
    fd.top = new FormAttachment( m_useCQL3Check, margin );
    fd.left = new FormAttachment( middle, 0 );
    m_useThriftIOCheck.setLayoutData( fd );

    m_useThriftIOCheck.addSelectionListener( new SelectionAdapter() {
      @Override
      public void widgetSelected( SelectionEvent e ) {
        m_useCQL3Check.setEnabled( !m_useThriftIOCheck.getSelection() );
        if ( m_useThriftIOCheck.getSelection() ) {
          m_useCQL3Check.setSelection( false );
        }
        checkWidgetsWithRespectToDriver( ConnectionFactory.Driver.LEGACY_THRIFT );
      }
    } );

    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.top = new FormAttachment( 0, 0 );
    fd.right = new FormAttachment( 100, 0 );
    fd.bottom = new FormAttachment( 100, 0 );
    wWriteComp.setLayoutData( fd );

    wWriteComp.layout();
    m_writeTab.setControl( wWriteComp );

    // show schema button
    m_showSchemaBut = new Button( wWriteComp, SWT.PUSH );
    m_showSchemaBut.setText( BaseMessages.getString( PKG, "CassandraOutputDialog.Schema.Button" ) ); //$NON-NLS-1$
    props.setLook( m_showSchemaBut );
    fd = new FormData();
    fd.right = new FormAttachment( 100, 0 );
    fd.bottom = new FormAttachment( 100, -margin * 2 );
    m_showSchemaBut.setLayoutData( fd );
    m_showSchemaBut.addSelectionListener( new SelectionAdapter() {
      @Override
      public void widgetSelected( SelectionEvent e ) {
        popupSchemaInfo();
      }
    } );

    // ---- start of the schema options tab ----
    m_schemaTab = new CTabItem( m_wTabFolder, SWT.NONE );
    m_schemaTab.setText( BaseMessages.getString( PKG, "CassandraOutputData.Tab.Schema" ) ); //$NON-NLS-1$

    Composite wSchemaComp = new Composite( m_wTabFolder, SWT.NONE );
    props.setLook( wSchemaComp );

    FormLayout schemaLayout = new FormLayout();
    schemaLayout.marginWidth = 3;
    schemaLayout.marginHeight = 3;
    wSchemaComp.setLayout( schemaLayout );

    // schema host line
    m_schemaHostLab = new Label( wSchemaComp, SWT.RIGHT );
    props.setLook( m_schemaHostLab );
    m_schemaHostLab.setText( BaseMessages.getString( PKG, "CassandraOutputDialog.SchemaHostname.Label" ) ); //$NON-NLS-1$
    m_schemaHostLab.setToolTipText( BaseMessages.getString( PKG, "CassandraOutputDialog.SchemaHostname.TipText" ) ); //$NON-NLS-1$
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.top = new FormAttachment( 0, margin );
    fd.right = new FormAttachment( middle, -margin );
    m_schemaHostLab.setLayoutData( fd );

    m_schemaHostText = new TextVar( transMeta, wSchemaComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( m_schemaHostText );
    m_schemaHostText.addModifyListener( new ModifyListener() {
      @Override
      public void modifyText( ModifyEvent e ) {
        m_schemaHostText.setToolTipText( transMeta.environmentSubstitute( m_schemaHostText.getText() ) );
      }
    } );
    m_schemaHostText.addModifyListener( lsMod );
    fd = new FormData();
    fd.right = new FormAttachment( 100, 0 );
    fd.top = new FormAttachment( 0, margin );
    fd.left = new FormAttachment( middle, 0 );
    m_schemaHostText.setLayoutData( fd );

    // schema port line
    m_schemaPortLab = new Label( wSchemaComp, SWT.RIGHT );
    props.setLook( m_schemaPortLab );
    m_schemaPortLab.setText( BaseMessages.getString( PKG, "CassandraOutputDialog.SchemaPort.Label" ) ); //$NON-NLS-1$
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.top = new FormAttachment( m_schemaHostText, margin );
    fd.right = new FormAttachment( middle, -margin );
    m_schemaPortLab.setLayoutData( fd );

    m_schemaPortText = new TextVar( transMeta, wSchemaComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( m_schemaPortText );
    m_schemaPortText.addModifyListener( new ModifyListener() {
      @Override
      public void modifyText( ModifyEvent e ) {
        m_schemaPortText.setToolTipText( transMeta.environmentSubstitute( m_schemaPortText.getText() ) );
      }
    } );
    m_schemaPortText.addModifyListener( lsMod );
    fd = new FormData();
    fd.right = new FormAttachment( 100, 0 );
    fd.top = new FormAttachment( m_schemaHostText, margin );
    fd.left = new FormAttachment( middle, 0 );
    m_schemaPortText.setLayoutData( fd );

    // create column family line
    m_createColumnFamilyLab = new Label( wSchemaComp, SWT.RIGHT );
    props.setLook( m_createColumnFamilyLab );
    m_createColumnFamilyLab.setText( BaseMessages.getString( PKG, "CassandraOutputDialog.CreateColumnFamily.Label" ) ); //$NON-NLS-1$
    m_createColumnFamilyLab.setToolTipText( BaseMessages.getString( PKG,
        "CassandraOutputDialog.CreateColumnFamily.TipText" ) ); //$NON-NLS-1$
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.top = new FormAttachment( m_schemaPortText, margin );
    fd.right = new FormAttachment( middle, -margin );
    m_createColumnFamilyLab.setLayoutData( fd );

    m_createColumnFamilyBut = new Button( wSchemaComp, SWT.CHECK );
    m_createColumnFamilyBut.setToolTipText( BaseMessages.getString( PKG,
        "CassandraOutputDialog.CreateColumnFamily.TipText" ) ); //$NON-NLS-1$
    props.setLook( m_createColumnFamilyBut );
    fd = new FormData();
    fd.right = new FormAttachment( 100, 0 );
    fd.top = new FormAttachment( m_schemaPortText, margin );
    fd.left = new FormAttachment( middle, 0 );
    m_createColumnFamilyBut.setLayoutData( fd );
    m_createColumnFamilyBut.addSelectionListener( new SelectionAdapter() {
      @Override
      public void widgetSelected( SelectionEvent e ) {
        m_currentMeta.setChanged();
      }
    } );

    // table creation with clause line
    m_withClauseLab = new Label( wSchemaComp, SWT.RIGHT );
    m_withClauseLab.setText( BaseMessages.getString( PKG, "CassandraOutputDialog.CreateTableWithClause.Label" ) ); //$NON-NLS-1$
    m_withClauseLab
        .setToolTipText( BaseMessages.getString( PKG, "CassandraOutputDialog.CreateTableWithClause.TipText" ) ); //$NON-NLS-1$
    props.setLook( m_withClauseLab );
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.top = new FormAttachment( m_createColumnFamilyBut, margin );
    fd.right = new FormAttachment( middle, -margin );
    m_withClauseLab.setLayoutData( fd );

    m_withClauseText = new TextVar( transMeta, wSchemaComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( m_withClauseText );
    m_withClauseText.addModifyListener( new ModifyListener() {
      @Override
      public void modifyText( ModifyEvent e ) {
        m_withClauseText.setToolTipText( transMeta.environmentSubstitute( m_withClauseText.getText() ) );
      }
    } );
    m_withClauseText.addModifyListener( lsMod );
    fd = new FormData();
    fd.right = new FormAttachment( 100, 0 );
    fd.top = new FormAttachment( m_createColumnFamilyBut, margin );
    fd.left = new FormAttachment( middle, 0 );
    m_withClauseText.setLayoutData( fd );

    // truncate column family line
    m_truncateColumnFamilyLab = new Label( wSchemaComp, SWT.RIGHT );
    props.setLook( m_truncateColumnFamilyLab );
    m_truncateColumnFamilyLab
        .setText( BaseMessages.getString( PKG, "CassandraOutputDialog.TruncateColumnFamily.Label" ) ); //$NON-NLS-1$
    m_truncateColumnFamilyLab.setToolTipText( BaseMessages.getString( PKG,
        "CassandraOutputDialog.TruncateColumnFamily.TipText" ) ); //$NON-NLS-1$
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.top = new FormAttachment( m_withClauseText, margin );
    fd.right = new FormAttachment( middle, -margin );
    m_truncateColumnFamilyLab.setLayoutData( fd );

    m_truncateColumnFamilyBut = new Button( wSchemaComp, SWT.CHECK );
    m_truncateColumnFamilyBut.setToolTipText( BaseMessages.getString( PKG,
        "CassandraOutputDialog.TruncateColumnFamily.TipText" ) ); //$NON-NLS-1$
    props.setLook( m_truncateColumnFamilyBut );
    fd = new FormData();
    fd.right = new FormAttachment( 100, 0 );
    fd.top = new FormAttachment( m_withClauseText, margin );
    fd.left = new FormAttachment( middle, 0 );
    m_truncateColumnFamilyBut.setLayoutData( fd );
    m_truncateColumnFamilyBut.addSelectionListener( new SelectionAdapter() {
      @Override
      public void widgetSelected( SelectionEvent e ) {
        m_currentMeta.setChanged();
      }
    } );

    // update column family meta data line
    m_updateColumnFamilyMetaDataLab = new Label( wSchemaComp, SWT.RIGHT );
    props.setLook( m_updateColumnFamilyMetaDataLab );
    m_updateColumnFamilyMetaDataLab.setText( BaseMessages.getString( PKG,
        "CassandraOutputDialog.UpdateColumnFamilyMetaData.Label" ) ); //$NON-NLS-1$
    m_updateColumnFamilyMetaDataLab.setToolTipText( BaseMessages.getString( PKG,
        "CassandraOutputDialog.UpdateColumnFamilyMetaData.TipText" ) ); //$NON-NLS-1$
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.top = new FormAttachment( m_truncateColumnFamilyBut, margin );
    fd.right = new FormAttachment( middle, -margin );
    m_updateColumnFamilyMetaDataLab.setLayoutData( fd );

    m_updateColumnFamilyMetaDataBut = new Button( wSchemaComp, SWT.CHECK );
    m_updateColumnFamilyMetaDataBut.setToolTipText( BaseMessages.getString( PKG,
        "CassandraOutputDialog.UpdateColumnFamilyMetaData.TipText" ) ); //$NON-NLS-1$
    props.setLook( m_updateColumnFamilyMetaDataBut );
    fd = new FormData();
    fd.right = new FormAttachment( 100, 0 );
    fd.top = new FormAttachment( m_truncateColumnFamilyBut, margin );
    fd.left = new FormAttachment( middle, 0 );
    m_updateColumnFamilyMetaDataBut.setLayoutData( fd );
    m_updateColumnFamilyMetaDataBut.addSelectionListener( new SelectionAdapter() {
      @Override
      public void widgetSelected( SelectionEvent e ) {
        m_currentMeta.setChanged();
      }
    } );

    // insert fields not in meta line
    m_insertFieldsNotInColumnFamMetaLab = new Label( wSchemaComp, SWT.RIGHT );
    props.setLook( m_insertFieldsNotInColumnFamMetaLab );
    m_insertFieldsNotInColumnFamMetaLab.setText( BaseMessages.getString( PKG,
        "CassandraOutputDialog.InsertFieldsNotInColumnFamMetaData.Label" ) ); //$NON-NLS-1$
    m_insertFieldsNotInColumnFamMetaLab.setToolTipText( BaseMessages.getString( PKG,
        "CassandraOutputDialog.InsertFieldsNotInColumnFamMetaData.TipText" ) ); //$NON-NLS-1$
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.top = new FormAttachment( m_updateColumnFamilyMetaDataBut, margin );
    fd.right = new FormAttachment( middle, -margin );
    m_insertFieldsNotInColumnFamMetaLab.setLayoutData( fd );

    m_insertFieldsNotInColumnFamMetaBut = new Button( wSchemaComp, SWT.CHECK );
    m_insertFieldsNotInColumnFamMetaBut.setToolTipText( BaseMessages.getString( PKG,
        "CassandraOutputDialog.InsertFieldsNotInColumnFamMetaData.TipText" ) ); //$NON-NLS-1$
    props.setLook( m_insertFieldsNotInColumnFamMetaBut );
    fd = new FormData();
    fd.right = new FormAttachment( 100, 0 );
    fd.top = new FormAttachment( m_updateColumnFamilyMetaDataBut, margin );
    fd.left = new FormAttachment( middle, 0 );
    m_insertFieldsNotInColumnFamMetaBut.setLayoutData( fd );
    m_insertFieldsNotInColumnFamMetaBut.addSelectionListener( new SelectionAdapter() {
      @Override
      public void widgetSelected( SelectionEvent e ) {
        m_currentMeta.setChanged();
      }
    } );

    // compression check box
    m_compressionLab = new Label( wSchemaComp, SWT.RIGHT );
    props.setLook( m_compressionLab );
    m_compressionLab.setText( BaseMessages.getString( PKG, "CassandraOutputDialog.UseCompression.Label" ) ); //$NON-NLS-1$
    m_compressionLab.setToolTipText( BaseMessages.getString( PKG, "CassandraOutputDialog.UseCompression.TipText" ) ); //$NON-NLS-1$
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.top = new FormAttachment( m_insertFieldsNotInColumnFamMetaBut, margin );
    fd.right = new FormAttachment( middle, -margin );
    m_compressionLab.setLayoutData( fd );

    m_useCompressionBut = new Button( wSchemaComp, SWT.CHECK );
    props.setLook( m_useCompressionBut );
    m_useCompressionBut.setToolTipText( BaseMessages.getString( PKG, "CassandraOutputDialog.UseCompression.TipText" ) ); //$NON-NLS-1$
    fd = new FormData();
    fd.right = new FormAttachment( 100, 0 );
    fd.left = new FormAttachment( middle, 0 );
    fd.top = new FormAttachment( m_insertFieldsNotInColumnFamMetaBut, margin );
    m_useCompressionBut.setLayoutData( fd );
    m_useCompressionBut.addSelectionListener( new SelectionAdapter() {
      @Override
      public void widgetSelected( SelectionEvent e ) {
        m_currentMeta.setChanged();
      }
    } );

    // Apriori CQL button
    m_aprioriCQLBut = new Button( wSchemaComp, SWT.PUSH | SWT.CENTER );
    props.setLook( m_aprioriCQLBut );
    m_aprioriCQLBut.setText( BaseMessages.getString( PKG, "CassandraOutputDialog.CQL.Button" ) ); //$NON-NLS-1$
    fd = new FormData();
    fd.right = new FormAttachment( 100, 0 );
    fd.bottom = new FormAttachment( 100, -margin * 2 );
    m_aprioriCQLBut.setLayoutData( fd );
    m_aprioriCQLBut.addSelectionListener( new SelectionAdapter() {
      @Override
      public void widgetSelected( SelectionEvent e ) {
        popupCQLEditor( lsMod );
      }
    } );

    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.top = new FormAttachment( 0, 0 );
    fd.right = new FormAttachment( 100, 0 );
    fd.bottom = new FormAttachment( 100, 0 );
    wSchemaComp.setLayoutData( fd );

    wSchemaComp.layout();
    m_schemaTab.setControl( wSchemaComp );

    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.top = new FormAttachment( m_stepnameText, margin );
    fd.right = new FormAttachment( 100, 0 );
    fd.bottom = new FormAttachment( 100, -50 );
    m_wTabFolder.setLayoutData( fd );

    // Buttons inherited from BaseStepDialog
    wOK = new Button( shell, SWT.PUSH );
    wOK.setText( BaseMessages.getString( PKG, "System.Button.OK" ) ); //$NON-NLS-1$

    wCancel = new Button( shell, SWT.PUSH );
    wCancel.setText( BaseMessages.getString( PKG, "System.Button.Cancel" ) ); //$NON-NLS-1$

    setButtonPositions( new Button[] { wOK, wCancel }, margin, m_wTabFolder );

    // Add listeners
    lsCancel = new Listener() {
      @Override
      public void handleEvent( Event e ) {
        cancel();
      }
    };

    lsOK = new Listener() {
      @Override
      public void handleEvent( Event e ) {
        ok();
      }
    };

    wCancel.addListener( SWT.Selection, lsCancel );
    wOK.addListener( SWT.Selection, lsOK );

    lsDef = new SelectionAdapter() {
      @Override
      public void widgetDefaultSelected( SelectionEvent e ) {
        ok();
      }
    };

    m_stepnameText.addSelectionListener( lsDef );

    // Detect X or ALT-F4 or something that kills this window...
    shell.addShellListener( new ShellAdapter() {
      @Override
      public void shellClosed( ShellEvent e ) {
        cancel();
      }
    } );

    m_wTabFolder.setSelection( 0 );
    setSize();

    getData();

    shell.open();
    while ( !shell.isDisposed() ) {
      if ( !display.readAndDispatch() ) {
        display.sleep();
      }
    }

    return stepname;
  }

  protected void setupColumnFamiliesCombo() {
    Connection conn = null;
    Keyspace kSpace = null;

    try {
      String hostS = transMeta.environmentSubstitute( m_hostText.getText() );
      String portS = transMeta.environmentSubstitute( m_portText.getText() );
      String userS = m_userText.getText();
      String passS = m_passText.getText();
      if ( !Const.isEmpty( userS ) && !Const.isEmpty( passS ) ) {
        userS = transMeta.environmentSubstitute( userS );
        passS = transMeta.environmentSubstitute( passS );
      }
      String keyspaceS = transMeta.environmentSubstitute( m_keyspaceText.getText() );

      try {
        Map<String, String> opts = new HashMap<String, String>();
        if ( m_useCQL3Check.getSelection() ) {
          opts.put( CassandraUtils.CQLOptions.CQLVERSION_OPTION, CassandraUtils.CQLOptions.CQL3_STRING );
        }
        conn =
            CassandraUtils.getCassandraConnection( hostS, Integer.parseInt( portS ), userS, passS,
                ConnectionFactory.Driver.LEGACY_THRIFT, opts );

        kSpace = conn.getKeyspace( keyspaceS );
      } catch ( InvalidRequestException ire ) {
        logError( BaseMessages.getString( PKG, "CassandraOutputDialog.Error.ProblemGettingSchemaInfo.Message" ) //$NON-NLS-1$
            + ":\n\n" + ire.why, ire ); //$NON-NLS-1$
        new ErrorDialog( shell, BaseMessages.getString( PKG,
            "CassandraOutputDialog.Error.ProblemGettingSchemaInfo.Title" ), //$NON-NLS-1$
            BaseMessages.getString( PKG, "CassandraOutputDialog.Error.ProblemGettingSchemaInfo.Message" ) //$NON-NLS-1$
                + ":\n\n" + ire.why, ire ); //$NON-NLS-1$
        return;
      }

      List<String> colFams = kSpace.getColumnFamilyNames();
      // List<String> colFams =
      // CassandraColumnMetaData.getColumnFamilyNames(conn);
      m_columnFamilyCombo.removeAll();
      for ( String famName : colFams ) {
        m_columnFamilyCombo.add( famName );
      }

    } catch ( Exception ex ) {
      logError( BaseMessages.getString( PKG, "CassandraOutputDialog.Error.ProblemGettingSchemaInfo.Message" ) //$NON-NLS-1$
          + ":\n\n" + ex.getMessage(), ex ); //$NON-NLS-1$
      new ErrorDialog( shell, BaseMessages
          .getString( PKG, "CassandraOutputDialog.Error.ProblemGettingSchemaInfo.Title" ), //$NON-NLS-1$
          BaseMessages.getString( PKG, "CassandraOutputDialog.Error.ProblemGettingSchemaInfo.Message" ) //$NON-NLS-1$
              + ":\n\n" + ex.getMessage(), ex ); //$NON-NLS-1$
    } finally {
      if ( conn != null ) {
        try {
          conn.closeConnection();
        } catch ( Exception e ) {
          // TODO popup another error dialog
          e.printStackTrace();
        }
      }
    }
  }

  protected void showEnterSelectionDialog() {
    StepMeta stepMeta = transMeta.findStep( stepname );

    String[] choices = null;
    if ( stepMeta != null ) {
      try {
        RowMetaInterface row = transMeta.getPrevStepFields( stepMeta );

        if ( row.size() == 0 ) {
          MessageDialog.openError( shell, BaseMessages.getString( PKG,
              "CassandraOutputData.Message.NoIncomingFields.Title" ), //$NON-NLS-1$
              BaseMessages.getString( PKG, "CassandraOutputData.Message.NoIncomingFields" ) ); //$NON-NLS-1$

          return;
        }

        choices = new String[row.size()];
        for ( int i = 0; i < row.size(); i++ ) {
          ValueMetaInterface vm = row.getValueMeta( i );
          choices[i] = vm.getName();
        }

        EnterSelectionDialog dialog =
            new EnterSelectionDialog( shell, choices, BaseMessages.getString( PKG,
                "CassandraOutputDialog.SelectKeyFieldsDialog.Title" ), //$NON-NLS-1$
                BaseMessages.getString( PKG, "CassandraOutputDialog.SelectKeyFieldsDialog.Message" ) ); //$NON-NLS-1$
        dialog.setMulti( true );
        if ( !Const.isEmpty( m_keyFieldCombo.getText() ) ) {
          String current = m_keyFieldCombo.getText();
          String[] parts = current.split( "," ); //$NON-NLS-1$
          int[] currentSelection = new int[parts.length];
          int count = 0;
          for ( String s : parts ) {
            int index = row.indexOfValue( s.trim() );
            if ( index >= 0 ) {
              currentSelection[count++] = index;
            }
          }

          dialog.setSelectedNrs( currentSelection );
        }

        dialog.open();

        int[] selected = dialog.getSelectionIndeces(); // SIC
        if ( selected != null && selected.length > 0 ) {
          StringBuilder newSelection = new StringBuilder();
          boolean first = true;
          for ( int i : selected ) {
            if ( first ) {
              newSelection.append( choices[i] );
              first = false;
            } else {
              newSelection.append( "," ).append( choices[i] ); //$NON-NLS-1$
            }
          }

          m_keyFieldCombo.setText( newSelection.toString() );
        }
      } catch ( KettleException ex ) {
        MessageDialog.openError( shell, BaseMessages.getString( PKG,
            "CassandraOutputData.Message.NoIncomingFields.Title" ), BaseMessages //$NON-NLS-1$
            .getString( PKG, "CassandraOutputData.Message.NoIncomingFields" ) ); //$NON-NLS-1$
      }
    }
  }

  protected void setupFieldsCombo() {
    // try and set up from incoming fields from previous step

    StepMeta stepMeta = transMeta.findStep( stepname );

    if ( stepMeta != null ) {
      try {
        RowMetaInterface row = transMeta.getPrevStepFields( stepMeta );

        if ( row.size() == 0 ) {
          MessageDialog.openError( shell, BaseMessages.getString( PKG,
              "CassandraOutputData.Message.NoIncomingFields.Title" ), //$NON-NLS-1$
              BaseMessages.getString( PKG, "CassandraOutputData.Message.NoIncomingFields" ) ); //$NON-NLS-1$

          return;
        }

        m_keyFieldCombo.removeAll();
        for ( int i = 0; i < row.size(); i++ ) {
          ValueMetaInterface vm = row.getValueMeta( i );
          m_keyFieldCombo.add( vm.getName() );
        }
      } catch ( KettleException ex ) {
        MessageDialog.openError( shell, BaseMessages.getString( PKG,
            "CassandraOutputData.Message.NoIncomingFields.Title" ), BaseMessages //$NON-NLS-1$
            .getString( PKG, "CassandraOutputData.Message.NoIncomingFields" ) ); //$NON-NLS-1$
      }
    }
  }

  protected void ok() {
    if ( Const.isEmpty( m_stepnameText.getText() ) ) {
      return;
    }

    stepname = m_stepnameText.getText();
    m_currentMeta.setCassandraHost( m_hostText.getText() );
    m_currentMeta.setCassandraPort( m_portText.getText() );
    m_currentMeta.setSchemaHost( m_schemaHostText.getText() );
    m_currentMeta.setSchemaPort( m_schemaPortText.getText() );
    m_currentMeta.setSocketTimeout( m_socketTimeoutText.getText() );
    m_currentMeta.setUsername( m_userText.getText() );
    m_currentMeta.setPassword( m_passText.getText() );
    m_currentMeta.setCassandraKeyspace( m_keyspaceText.getText() );
    m_currentMeta.setColumnFamilyName( m_columnFamilyCombo.getText() );
    m_currentMeta.setConsistency( m_consistencyText.getText() );
    m_currentMeta.setBatchSize( m_batchSizeText.getText() );
    m_currentMeta.setCQLBatchInsertTimeout( m_batchInsertTimeoutText.getText() );
    m_currentMeta.setCQLSubBatchSize( m_subBatchSizeText.getText() );
    m_currentMeta.setKeyField( m_keyFieldCombo.getText() );

    m_currentMeta.setCreateColumnFamily( m_createColumnFamilyBut.getSelection() );
    m_currentMeta.setTruncateColumnFamily( m_truncateColumnFamilyBut.getSelection() );
    m_currentMeta.setUpdateCassandraMeta( m_updateColumnFamilyMetaDataBut.getSelection() );
    m_currentMeta.setInsertFieldsNotInMeta( m_insertFieldsNotInColumnFamMetaBut.getSelection() );
    m_currentMeta.setUseCompression( m_useCompressionBut.getSelection() );
    m_currentMeta.setAprioriCQL( m_aprioriCQL );
    m_currentMeta.setUseThriftIO( m_useThriftIOCheck.getSelection() );
    m_currentMeta.setUseCQL3( m_useCQL3Check.getSelection() );
    m_currentMeta.setCreateTableClause( m_withClauseText.getText() );
    m_currentMeta.setDontComplainAboutAprioriCQLFailing( m_dontComplain );
    m_currentMeta.setUseUnloggedBatches( m_unloggedBatchBut.getSelection() );

    if ( !Const.isEmpty( m_ttlValueText.getText() ) ) {
      m_currentMeta.setTTL( m_ttlValueText.getText() );
      m_currentMeta.setTTLUnit( m_ttlUnitsCombo.getText() );
    }

    if ( !m_originalMeta.equals( m_currentMeta ) ) {
      m_currentMeta.setChanged();
      changed = m_currentMeta.hasChanged();
    }

    dispose();
  }

  protected void cancel() {
    stepname = null;
    m_currentMeta.setChanged( changed );

    dispose();
  }

  protected void popupCQLEditor( ModifyListener lsMod ) {

    EnterCQLDialog ecd =
        new EnterCQLDialog( shell, transMeta, lsMod, BaseMessages.getString( PKG, "CassandraOutputDialog.CQL.Button" ), //$NON-NLS-1$
            m_aprioriCQL, m_dontComplain );

    m_aprioriCQL = ecd.open();
    m_dontComplain = ecd.getDontComplainStatus();
  }

  protected void popupSchemaInfo() {

    Connection conn = null;
    Keyspace kSpace = null;
    try {
      String hostS = transMeta.environmentSubstitute( m_hostText.getText() );
      String portS = transMeta.environmentSubstitute( m_portText.getText() );
      String userS = m_userText.getText();
      String passS = m_passText.getText();
      if ( !Const.isEmpty( userS ) && !Const.isEmpty( passS ) ) {
        userS = transMeta.environmentSubstitute( userS );
        passS = transMeta.environmentSubstitute( passS );
      }
      String keyspaceS = transMeta.environmentSubstitute( m_keyspaceText.getText() );

      try {
        Map<String, String> opts = new HashMap<String, String>();
        if ( m_useCQL3Check.getSelection() ) {
          opts.put( CassandraUtils.CQLOptions.CQLVERSION_OPTION, CassandraUtils.CQLOptions.CQL3_STRING );
        }

        conn =
            CassandraUtils.getCassandraConnection( hostS, Integer.parseInt( portS ), userS, passS,
                ConnectionFactory.Driver.LEGACY_THRIFT, opts );

        conn.setHosts( hostS );
        conn.setDefaultPort( Integer.parseInt( portS ) );
        conn.setUsername( userS );
        conn.setPassword( passS );
        kSpace = conn.getKeyspace( keyspaceS );

        // conn = CassandraOutputData.getCassandraConnection(hostS,
        // Integer.parseInt(portS), userS, passS);
        //
        // conn.setKeyspace(keyspaceS);
      } catch ( InvalidRequestException ire ) {
        logError( BaseMessages.getString( PKG, "CassandraOutputDialog.Error.ProblemGettingSchemaInfo.Message" ) //$NON-NLS-1$
            + ":\n\n" + ire.why, ire ); //$NON-NLS-1$
        new ErrorDialog( shell, BaseMessages.getString( PKG,
            "CassandraOutputDialog.Error.ProblemGettingSchemaInfo.Title" ), //$NON-NLS-1$
            BaseMessages.getString( PKG, "CassandraOutputDialog.Error.ProblemGettingSchemaInfo.Message" ) //$NON-NLS-1$
                + ":\n\n" + ire.why, ire ); //$NON-NLS-1$
        return;
      }

      String colFam = transMeta.environmentSubstitute( m_columnFamilyCombo.getText() );
      if ( Const.isEmpty( colFam ) ) {
        throw new Exception( "No colummn family (table) name specified!" ); //$NON-NLS-1$
      }
      colFam = m_useCQL3Check.getSelection() ? CassandraUtils.cql3MixedCaseQuote( colFam ) : colFam;

      // if (!CassandraColumnMetaData.columnFamilyExists(conn, colFam)) {
      if ( !kSpace.columnFamilyExists( colFam ) ) {
        throw new Exception( "The column family '" + colFam + "' does not " //$NON-NLS-1$ //$NON-NLS-2$
            + "seem to exist in the keyspace '" + keyspaceS ); //$NON-NLS-1$
      }

      CassandraColumnMetaData cassMeta = (CassandraColumnMetaData) kSpace.getColumnFamilyMetaData( colFam );
      // CassandraColumnMetaData cassMeta = new CassandraColumnMetaData(conn,
      // colFam);
      String schemaDescription = cassMeta.getSchemaDescription();
      ShowMessageDialog smd =
          new ShowMessageDialog( shell, SWT.ICON_INFORMATION | SWT.OK, "Schema info", schemaDescription, true ); //$NON-NLS-1$
      smd.open();
    } catch ( Exception e1 ) {
      logError( BaseMessages.getString( PKG, "CassandraOutputDialog.Error.ProblemGettingSchemaInfo.Message" ) //$NON-NLS-1$
          + ":\n\n" + e1.getMessage(), e1 ); //$NON-NLS-1$
      new ErrorDialog( shell, BaseMessages
          .getString( PKG, "CassandraOutputDialog.Error.ProblemGettingSchemaInfo.Title" ), //$NON-NLS-1$
          BaseMessages.getString( PKG, "CassandraOutputDialog.Error.ProblemGettingSchemaInfo.Message" ) //$NON-NLS-1$
              + ":\n\n" + e1.getMessage(), e1 ); //$NON-NLS-1$
    } finally {
      if ( conn != null ) {
        try {
          conn.closeConnection();
        } catch ( Exception e ) {
          // TODO popup another error dialog
          e.printStackTrace();
        }
      }
    }
  }

  protected void getData() {

    if ( !Const.isEmpty( m_currentMeta.getCassandraHost() ) ) {
      m_hostText.setText( m_currentMeta.getCassandraHost() );
    }

    if ( !Const.isEmpty( m_currentMeta.getCassandraPort() ) ) {
      m_portText.setText( m_currentMeta.getCassandraPort() );
    }

    if ( !Const.isEmpty( m_currentMeta.getSchemaHost() ) ) {
      m_schemaHostText.setText( m_currentMeta.getSchemaHost() );
    }

    if ( !Const.isEmpty( m_currentMeta.getSchemaPort() ) ) {
      m_schemaPortText.setText( m_currentMeta.getSchemaPort() );
    }

    if ( !Const.isEmpty( m_currentMeta.getSocketTimeout() ) ) {
      m_socketTimeoutText.setText( m_currentMeta.getSocketTimeout() );
    }

    if ( !Const.isEmpty( m_currentMeta.getUsername() ) ) {
      m_userText.setText( m_currentMeta.getUsername() );
    }

    if ( !Const.isEmpty( m_currentMeta.getPassword() ) ) {
      m_passText.setText( m_currentMeta.getPassword() );
    }

    if ( !Const.isEmpty( m_currentMeta.getCassandraKeyspace() ) ) {
      m_keyspaceText.setText( m_currentMeta.getCassandraKeyspace() );
    }

    if ( !Const.isEmpty( m_currentMeta.getColumnFamilyName() ) ) {
      m_columnFamilyCombo.setText( m_currentMeta.getColumnFamilyName() );
    }

    if ( !Const.isEmpty( m_currentMeta.getConsistency() ) ) {
      m_consistencyText.setText( m_currentMeta.getConsistency() );
    }

    if ( !Const.isEmpty( m_currentMeta.getBatchSize() ) ) {
      m_batchSizeText.setText( m_currentMeta.getBatchSize() );
    }

    if ( !Const.isEmpty( m_currentMeta.getCQLBatchInsertTimeout() ) ) {
      m_batchInsertTimeoutText.setText( m_currentMeta.getCQLBatchInsertTimeout() );
    }

    if ( !Const.isEmpty( m_currentMeta.getCQLSubBatchSize() ) ) {
      m_subBatchSizeText.setText( m_currentMeta.getCQLSubBatchSize() );
    }

    if ( !Const.isEmpty( m_currentMeta.getKeyField() ) ) {
      m_keyFieldCombo.setText( m_currentMeta.getKeyField() );
    }

    if ( !Const.isEmpty( m_currentMeta.getCreateTableWithClause() ) ) {
      m_withClauseText.setText( m_currentMeta.getCreateTableWithClause() );
    }

    m_createColumnFamilyBut.setSelection( m_currentMeta.getCreateColumnFamily() );
    m_truncateColumnFamilyBut.setSelection( m_currentMeta.getTruncateColumnFamily() );
    m_updateColumnFamilyMetaDataBut.setSelection( m_currentMeta.getUpdateCassandraMeta() );
    m_insertFieldsNotInColumnFamMetaBut.setSelection( m_currentMeta.getInsertFieldsNotInMeta() );
    m_useCompressionBut.setSelection( m_currentMeta.getUseCompression() );
    m_useThriftIOCheck.setSelection( m_currentMeta.getUseThriftIO() );
    m_useCQL3Check.setSelection( m_currentMeta.getUseCQL3() );
    m_unloggedBatchBut.setSelection( m_currentMeta.getUseUnloggedBatch() );

    m_dontComplain = m_currentMeta.getDontComplainAboutAprioriCQLFailing();

    m_aprioriCQL = m_currentMeta.getAprioriCQL();
    if ( m_aprioriCQL == null ) {
      m_aprioriCQL = ""; //$NON-NLS-1$
    }

    if ( m_useCQL3Check.getSelection() ) {
      m_useThriftIOCheck.setSelection( false );
      m_getFieldsBut.setText( BaseMessages.getString( PKG, "CassandraOutputDialog.SelectFields.Button" ) ); //$NON-NLS-1$
      m_keyFieldLab.setText( BaseMessages.getString( PKG, "CassandraOutputDialog.KeyFields.Label" ) ); //$NON-NLS-1$
    } else {
      m_getFieldsBut.setText( " " //$NON-NLS-1$
          + BaseMessages.getString( PKG, "CassandraOutputDialog.GetFields.Button" ) + " " ); //$NON-NLS-1$ //$NON-NLS-2$
      m_keyFieldLab.setText( BaseMessages.getString( PKG, "CassandraOutputDialog.KeyField.Label" ) ); //$NON-NLS-1$

      // unlogged batches is CQL 3 only
      m_unloggedBatchBut.setSelection( false );
      m_unloggedBatchBut.setEnabled( false );
    }

    if ( !Const.isEmpty( m_currentMeta.getTTL() ) ) {
      m_ttlValueText.setText( m_currentMeta.getTTL() );
      m_ttlUnitsCombo.setText( m_currentMeta.getTTLUnit() );
      m_ttlValueText.setEnabled( m_ttlUnitsCombo.getSelectionIndex() > 0 );
    }

    checkWidgetsWithRespectToDriver( ConnectionFactory.Driver.LEGACY_THRIFT );
  }

  private void checkWidgetsWithRespectToDriver( ConnectionFactory.Driver d ) {
    Connection c = ConnectionFactory.getFactory().getConnection( d );

    m_useCQL3Check.setEnabled( c.supportsCQL() );
    if ( !c.supportsCQL() ) {
      m_useCQL3Check.setSelection( false );
      m_unloggedBatchBut.setSelection( false );
      m_unloggedBatchBut.setEnabled( false );
      m_useThriftIOCheck.setEnabled( true );
      m_useThriftIOCheck.setSelection( true );
    }

    m_useThriftIOCheck.setEnabled( c.supportsNonCQL() );
    if ( !c.supportsNonCQL() ) {
      m_useThriftIOCheck.setSelection( false );
    }
  }

  private void checkPasswordVisible() {
    String password = m_passText.getText();
    ArrayList<String> list = new ArrayList<String>();
    StringUtil.getUsedVariables( password, list, true );
    if ( list.size() == 0 ) {
      m_passText.setEchoChar( '*' );
    } else {
      m_passText.setEchoChar( '\0' ); // show everything
    }
  }
}
